package com.javaproject.application.security.authentication;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory that resolves the correct {@link AuthenticationStrategy} by name.
 * <p>
 * All strategy beans are auto-discovered via constructor injection.
 * To add a new authentication method, simply create a new class that
 * implements {@link AuthenticationStrategy} and annotate it with {@code @Component}.
 */
@Component
public class AuthenticationStrategyFactory {

    private final Map<String, AuthenticationStrategy> strategyMap;

    public AuthenticationStrategyFactory(List<AuthenticationStrategy> strategies) {
        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(
                        AuthenticationStrategy::getStrategyName,
                        Function.identity()
                ));
    }

    /**
     * @param strategyName e.g. "JWT", "OAUTH2"
     * @return the matching strategy
     * @throws IllegalArgumentException if no strategy is registered for the name
     */
    public AuthenticationStrategy getStrategy(String strategyName) {
        AuthenticationStrategy strategy = strategyMap.get(strategyName.toUpperCase());
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported authentication method: " + strategyName);
        }
        return strategy;
    }
}
