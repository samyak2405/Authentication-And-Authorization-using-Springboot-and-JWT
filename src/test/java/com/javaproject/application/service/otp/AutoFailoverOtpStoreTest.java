package com.javaproject.application.service.otp;

import com.javaproject.application.model.User;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AutoFailoverOtpStoreTest {

    @Test
    void usesDbBackendWhenConfigured() {
        TrackingStore redisStore = new TrackingStore();
        TrackingStore dbStore = new TrackingStore();
        AutoFailoverOtpStore store = new AutoFailoverOtpStore(redisStore, dbStore, new SimpleMeterRegistry());
        ReflectionTestUtils.setField(store, "backendMode", "db");

        store.checkResendAllowed(request());

        assertEquals(0, redisStore.calls.get());
        assertEquals(1, dbStore.calls.get());
    }

    @Test
    void fallsBackToDbWhenRedisFailsInAutoMode() {
        TrackingStore redisStore = new TrackingStore();
        redisStore.throwRedisFailure = true;
        TrackingStore dbStore = new TrackingStore();
        AutoFailoverOtpStore store = new AutoFailoverOtpStore(redisStore, dbStore, new SimpleMeterRegistry());
        ReflectionTestUtils.setField(store, "backendMode", "auto");

        store.verify(verifyRequest());

        assertEquals(1, redisStore.calls.get());
        assertEquals(1, dbStore.calls.get());
    }

    @Test
    void opensCircuitAfterConfiguredFailures() {
        TrackingStore redisStore = new TrackingStore();
        redisStore.throwRedisFailure = true;
        TrackingStore dbStore = new TrackingStore();
        AutoFailoverOtpStore store = new AutoFailoverOtpStore(redisStore, dbStore, new SimpleMeterRegistry());
        ReflectionTestUtils.setField(store, "backendMode", "auto");
        ReflectionTestUtils.setField(store, "redisFailuresThreshold", 1);
        ReflectionTestUtils.setField(store, "redisCircuitOpenSeconds", 60);

        store.verify(verifyRequest());
        store.verify(verifyRequest());

        assertEquals(1, redisStore.calls.get());
        assertEquals(2, dbStore.calls.get());
    }

    private static OtpStore.OtpResendCheckRequest request() {
        return OtpStore.OtpResendCheckRequest.builder()
                .user(user())
                .purpose("OTP")
                .resendCooldownSeconds(60)
                .now(OffsetDateTime.now())
                .build();
    }

    private static OtpStore.OtpVerifyRequest verifyRequest() {
        return OtpStore.OtpVerifyRequest.builder()
                .user(user())
                .purpose("OTP")
                .providedTokenHash("hash")
                .maxAttempts(5)
                .now(OffsetDateTime.now())
                .build();
    }

    private static User user() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");
        return user;
    }

    private static class TrackingStore implements OtpStore {
        private final AtomicInteger calls = new AtomicInteger(0);
        private boolean throwRedisFailure = false;

        @Override
        public OtpIssueResult issue(OtpIssueRequest request) {
            calls.incrementAndGet();
            if (throwRedisFailure) {
                throw new RedisConnectionFailureException("redis down");
            }
            return OtpIssueResult.builder()
                    .issuedAt(request.getIssuedAt())
                    .expiresAt(request.getExpiresAt())
                    .build();
        }

        @Override
        public OtpResendCheckResult checkResendAllowed(OtpResendCheckRequest request) {
            calls.incrementAndGet();
            if (throwRedisFailure) {
                throw new RedisConnectionFailureException("redis down");
            }
            return OtpResendCheckResult.builder()
                    .resendAllowed(true)
                    .waitSeconds(0)
                    .build();
        }

        @Override
        public OtpVerifyResult verify(OtpVerifyRequest request) {
            calls.incrementAndGet();
            if (throwRedisFailure) {
                throw new RedisConnectionFailureException("redis down");
            }
            return OtpVerifyResult.builder()
                    .status(OtpVerifyStatus.VERIFIED)
                    .attempts(1)
                    .build();
        }

        @Override
        public void invalidateActive(User user, String purpose, OffsetDateTime now) {
            calls.incrementAndGet();
            if (throwRedisFailure) {
                throw new RedisConnectionFailureException("redis down");
            }
        }
    }
}
