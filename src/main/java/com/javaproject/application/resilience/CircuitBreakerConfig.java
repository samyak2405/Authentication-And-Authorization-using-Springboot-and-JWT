package com.javaproject.application.resilience;

import java.time.Duration;

/**
 * Configuration for a {@link CircuitBreaker} instance.
 * Use the {@link #builder()} to create with custom settings, or {@link #defaults()} for sensible defaults.
 *
 * <p>Example:
 * <pre>{@code
 * CircuitBreakerConfig config = CircuitBreakerConfig.builder()
 *         .failureThreshold(5)
 *         .cooldownDuration(Duration.ofSeconds(60))
 *         .build();
 * }</pre>
 */
public class CircuitBreakerConfig {

    private final int failureThreshold;
    private final Duration cooldownDuration;
    private final int halfOpenPermittedCalls;

    private CircuitBreakerConfig(Builder builder) {
        this.failureThreshold = builder.failureThreshold;
        this.cooldownDuration = builder.cooldownDuration;
        this.halfOpenPermittedCalls = builder.halfOpenPermittedCalls;
    }

    /** Default config: 3 failures, 30s cooldown, 1 half-open call. */
    public static CircuitBreakerConfig defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public int getFailureThreshold() {
        return failureThreshold;
    }

    public Duration getCooldownDuration() {
        return cooldownDuration;
    }

    public int getHalfOpenPermittedCalls() {
        return halfOpenPermittedCalls;
    }

    public static class Builder {
        private int failureThreshold = 3;
        private Duration cooldownDuration = Duration.ofSeconds(30);
        private int halfOpenPermittedCalls = 1;

        private Builder() {}

        /** Number of consecutive failures before the circuit opens. Default: 3 */
        public Builder failureThreshold(int failureThreshold) {
            if (failureThreshold < 1) {
                throw new IllegalArgumentException("failureThreshold must be >= 1");
            }
            this.failureThreshold = failureThreshold;
            return this;
        }

        /** Duration the circuit stays open before allowing half-open retries. Default: 30s */
        public Builder cooldownDuration(Duration cooldownDuration) {
            if (cooldownDuration == null || cooldownDuration.isNegative() || cooldownDuration.isZero()) {
                throw new IllegalArgumentException("cooldownDuration must be positive");
            }
            this.cooldownDuration = cooldownDuration;
            return this;
        }

        /** Number of calls permitted in half-open state before deciding to close or re-open. Default: 1 */
        public Builder halfOpenPermittedCalls(int halfOpenPermittedCalls) {
            if (halfOpenPermittedCalls < 1) {
                throw new IllegalArgumentException("halfOpenPermittedCalls must be >= 1");
            }
            this.halfOpenPermittedCalls = halfOpenPermittedCalls;
            return this;
        }

        public CircuitBreakerConfig build() {
            return new CircuitBreakerConfig(this);
        }
    }
}
