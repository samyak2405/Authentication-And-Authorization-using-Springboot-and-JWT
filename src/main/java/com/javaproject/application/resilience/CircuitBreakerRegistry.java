package com.javaproject.application.resilience;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spring-managed registry for creating and retrieving {@link CircuitBreaker} instances by name.
 *
 * <p>Each external service should have its own named circuit breaker. The registry ensures
 * only one breaker exists per name (thread-safe).
 *
 * <p>Usage:
 * <pre>{@code
 * @RequiredArgsConstructor
 * public class PaymentClient {
 *     private final CircuitBreakerRegistry cbRegistry;
 *
 *     public PaymentResponse charge(PaymentRequest req) {
 *         return cbRegistry.get("payment-service").execute(
 *             () -> httpClient.post("/charge", req, PaymentResponse.class),
 *             () -> PaymentResponse.fallback("Service unavailable")
 *         );
 *     }
 * }
 * }</pre>
 */
@Slf4j
@Component
public class CircuitBreakerRegistry {

    private final ConcurrentHashMap<String, CircuitBreaker> breakers = new ConcurrentHashMap<>();

    /**
     * Get or create a circuit breaker with the given name and default config.
     */
    public CircuitBreaker get(String name) {
        return get(name, CircuitBreakerConfig.defaults());
    }

    /**
     * Get or create a circuit breaker with the given name and custom config.
     * If a breaker already exists for this name, the existing one is returned (config is not updated).
     */
    public CircuitBreaker get(String name, CircuitBreakerConfig config) {
        return breakers.computeIfAbsent(name, n -> {
            log.info("Creating circuit breaker [{}] with failureThreshold={}, cooldown={}s",
                    n, config.getFailureThreshold(), config.getCooldownDuration().getSeconds());
            return new CircuitBreaker(n, config);
        });
    }

    /**
     * Returns an unmodifiable view of all registered circuit breakers.
     * Useful for monitoring/actuator endpoints.
     */
    public Map<String, CircuitBreaker> getAll() {
        return Collections.unmodifiableMap(breakers);
    }

    /**
     * Remove a circuit breaker from the registry.
     */
    public void remove(String name) {
        breakers.remove(name);
        log.info("Removed circuit breaker [{}] from registry", name);
    }
}
