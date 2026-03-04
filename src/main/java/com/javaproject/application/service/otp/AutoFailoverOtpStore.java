package com.javaproject.application.service.otp;

import com.javaproject.application.model.User;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class AutoFailoverOtpStore implements OtpStore {

    private final @Qualifier("redisOtpStore") OtpStore redisOtpStore;
    private final @Qualifier("dbOtpStore") OtpStore dbOtpStore;
    private final MeterRegistry meterRegistry;

    @Value("${app.notifications.registration-otp.backend:auto}")
    private String backendMode;

    @Value("${app.notifications.registration-otp.redis-failures-threshold:3}")
    private int redisFailuresThreshold;

    @Value("${app.notifications.registration-otp.redis-circuit-open-seconds:30}")
    private int redisCircuitOpenSeconds;

    private final AtomicInteger redisConsecutiveFailures = new AtomicInteger(0);
    private final AtomicLong circuitOpenedAtMillis = new AtomicLong(0);

    @Override
    public OtpIssueResult issue(OtpIssueRequest request) {
        return executeWithFailover("issue", () -> redisOtpStore.issue(request), () -> dbOtpStore.issue(request));
    }

    @Override
    public OtpResendCheckResult checkResendAllowed(OtpResendCheckRequest request) {
        return executeWithFailover(
                "resend_check",
                () -> redisOtpStore.checkResendAllowed(request),
                () -> dbOtpStore.checkResendAllowed(request)
        );
    }

    @Override
    public OtpVerifyResult verify(OtpVerifyRequest request) {
        return executeWithFailover("verify", () -> redisOtpStore.verify(request), () -> dbOtpStore.verify(request));
    }

    @Override
    public void invalidateActive(User user, String purpose, OffsetDateTime now) {
        executeWithFailover(
                "invalidate",
                () -> {
                    redisOtpStore.invalidateActive(user, purpose, now);
                    return null;
                },
                () -> {
                    dbOtpStore.invalidateActive(user, purpose, now);
                    return null;
                }
        );
    }

    private <T> T executeWithFailover(String operation, Supplier<T> redisCall, Supplier<T> dbCall) {
        BackendMode mode = BackendMode.from(backendMode);
        if (mode == BackendMode.DB) {
            return executeAndRecord(operation, "db", dbCall);
        }
        if (mode == BackendMode.REDIS) {
            return executeAndRecord(operation, "redis", redisCall);
        }

        if (isCircuitOpen()) {
            meterRegistry.counter("auth.otp.store.fallback", "operation", operation, "reason", "circuit_open").increment();
            return executeAndRecord(operation, "db", dbCall);
        }

        try {
            T result = executeAndRecord(operation, "redis", redisCall);
            redisConsecutiveFailures.set(0);
            circuitOpenedAtMillis.set(0);
            return result;
        } catch (RuntimeException ex) {
            if (!isRedisAvailabilityError(ex)) {
                throw ex;
            }

            int failures = redisConsecutiveFailures.incrementAndGet();
            meterRegistry.counter("auth.otp.store.redis_error", "operation", operation).increment();
            log.warn("Redis OTP store failure (operation={}, consecutiveFailures={}). Falling back to DB. Cause={}",
                    operation, failures, ex.getMessage());

            if (failures >= redisFailuresThreshold) {
                circuitOpenedAtMillis.compareAndSet(0, System.currentTimeMillis());
            }

            meterRegistry.counter("auth.otp.store.fallback", "operation", operation, "reason", "redis_error").increment();
            return executeAndRecord(operation, "db", dbCall);
        }
    }

    private <T> T executeAndRecord(String operation, String backend, Supplier<T> call) {
        Timer.Sample timerSample = Timer.start(meterRegistry);
        try {
            T result = call.get();
            meterRegistry.counter("auth.otp.store.success", "operation", operation, "backend", backend).increment();
            return result;
        } catch (RuntimeException ex) {
            meterRegistry.counter("auth.otp.store.error", "operation", operation, "backend", backend).increment();
            throw ex;
        } finally {
            timerSample.stop(Timer.builder("auth.otp.store.latency")
                    .tag("operation", operation)
                    .tag("backend", backend)
                    .register(meterRegistry));
        }
    }

    private boolean isCircuitOpen() {
        long openedAt = circuitOpenedAtMillis.get();
        if (openedAt == 0) {
            return false;
        }
        long openForMillis = redisCircuitOpenSeconds * 1000L;
        if (System.currentTimeMillis() - openedAt < openForMillis) {
            return true;
        }
        circuitOpenedAtMillis.set(0);
        redisConsecutiveFailures.set(0);
        return false;
    }

    private boolean isRedisAvailabilityError(RuntimeException ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof RedisConnectionFailureException || current instanceof DataAccessException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private enum BackendMode {
        REDIS,
        DB,
        AUTO;

        private static BackendMode from(String rawMode) {
            if (rawMode == null || rawMode.isBlank()) {
                return AUTO;
            }
            try {
                return BackendMode.valueOf(rawMode.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return AUTO;
            }
        }
    }
}
