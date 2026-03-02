package com.javaproject.application.resilience;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * A lightweight, thread-safe circuit breaker for protecting external service calls.
 *
 * <p>States:
 * <ul>
 *   <li><b>CLOSED</b> — normal operation, calls pass through</li>
 *   <li><b>OPEN</b> — calls are short-circuited to fallback (after consecutive failures exceed threshold)</li>
 *   <li><b>HALF_OPEN</b> — limited calls allowed to test recovery</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 * CircuitBreaker cb = new CircuitBreaker("payment-service", config);
 *
 * // With return value + fallback
 * String result = cb.execute(
 *     () -> httpClient.get("/api/payment"),
 *     () -> "default-response"
 * );
 *
 * // Fire-and-forget (no fallback needed)
 * cb.executeVoid(() -> notificationService.send(event));
 * }</pre>
 */
public class CircuitBreaker {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreaker.class);

    public enum State { CLOSED, OPEN, HALF_OPEN }

    private final String name;
    private final CircuitBreakerConfig config;

    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicLong lastFailureTimestamp = new AtomicLong(0);
    private final AtomicInteger halfOpenSuccesses = new AtomicInteger(0);

    public CircuitBreaker(String name, CircuitBreakerConfig config) {
        this.name = name;
        this.config = config;
    }

    /**
     * Execute an action with a fallback. If the circuit is open, the fallback is returned immediately.
     *
     * @param action   the external call to protect
     * @param fallback the fallback to use when the circuit is open or the action fails
     * @return the result of the action, or the fallback value
     */
    public <T> T execute(Supplier<T> action, Supplier<T> fallback) {
        if (!allowRequest()) {
            log.debug("[{}] Circuit OPEN — short-circuiting to fallback", name);
            return fallback.get();
        }

        try {
            T result = action.get();
            onSuccess();
            return result;
        } catch (Exception e) {
            onFailure(e);
            return fallback.get();
        }
    }

    /**
     * Execute a void action (fire-and-forget). Failures are logged but not propagated.
     *
     * @param action the external call to protect
     */
    public void executeVoid(Runnable action) {
        if (!allowRequest()) {
            log.debug("[{}] Circuit OPEN — skipping void action", name);
            return;
        }

        try {
            action.run();
            onSuccess();
        } catch (Exception e) {
            onFailure(e);
        }
    }

    // ── State management ─────────────────────────────────────────────────

    private boolean allowRequest() {
        State currentState = evaluateState();
        return currentState == State.CLOSED || currentState == State.HALF_OPEN;
    }

    /**
     * Evaluates and potentially transitions the current state.
     * OPEN → HALF_OPEN when cooldown has elapsed.
     */
    private State evaluateState() {
        State current = state.get();
        if (current == State.OPEN) {
            long elapsed = System.currentTimeMillis() - lastFailureTimestamp.get();
            if (elapsed >= config.getCooldownDuration().toMillis()) {
                if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                    halfOpenSuccesses.set(0);
                    log.info("[{}] Circuit breaker transitioning OPEN → HALF_OPEN (cooldown {}s elapsed)",
                            name, config.getCooldownDuration().getSeconds());
                }
                return State.HALF_OPEN;
            }
        }
        return current;
    }

    private void onSuccess() {
        State current = state.get();
        if (current == State.HALF_OPEN) {
            int successes = halfOpenSuccesses.incrementAndGet();
            if (successes >= config.getHalfOpenPermittedCalls()) {
                state.set(State.CLOSED);
                consecutiveFailures.set(0);
                halfOpenSuccesses.set(0);
                log.info("[{}] Circuit breaker CLOSED — service recovered after {} successful half-open call(s)",
                        name, successes);
            }
        } else if (current == State.CLOSED && consecutiveFailures.get() > 0) {
            consecutiveFailures.set(0);
        }
    }

    private void onFailure(Exception e) {
        int failures = consecutiveFailures.incrementAndGet();
        lastFailureTimestamp.set(System.currentTimeMillis());

        State current = state.get();
        if (current == State.HALF_OPEN) {
            // Half-open retry failed — back to OPEN
            state.set(State.OPEN);
            log.warn("[{}] Circuit breaker re-opened (half-open call failed): {}", name, e.getMessage());
        } else if (failures >= config.getFailureThreshold()) {
            if (state.compareAndSet(State.CLOSED, State.OPEN)) {
                log.warn("[{}] Circuit breaker OPEN after {} consecutive failures. "
                                + "Skipping calls for {}s. Last error: {}",
                        name, failures, config.getCooldownDuration().getSeconds(), e.getMessage());
            }
        } else {
            log.warn("[{}] Call failed (failure {}/{}): {}",
                    name, failures, config.getFailureThreshold(), e.getMessage());
        }
    }

    // ── Observability ────────────────────────────────────────────────────

    public String getName() {
        return name;
    }

    public State getState() {
        return evaluateState();
    }

    public int getConsecutiveFailures() {
        return consecutiveFailures.get();
    }

    /** Force the circuit to the CLOSED state. Useful for admin/management endpoints. */
    public void reset() {
        state.set(State.CLOSED);
        consecutiveFailures.set(0);
        halfOpenSuccesses.set(0);
        lastFailureTimestamp.set(0);
        log.info("[{}] Circuit breaker manually reset to CLOSED", name);
    }
}
