package com.javaproject.application.service.impl;

import com.javaproject.application.dto.SecurityConfigDto;
import com.javaproject.application.resilience.CircuitBreaker;
import com.javaproject.application.resilience.CircuitBreakerConfig;
import com.javaproject.application.resilience.CircuitBreakerRegistry;
import tools.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCacheService {

    private static final String AUTH_PARAM_KEY_PREFIX = "auth:param:";
    private static final String SECURITY_CONFIG_KEY_PREFIX = "security:config:";
    private static final String CIRCUIT_BREAKER_NAME = "redis-cache";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @Value("${cache.redis.ttl-minutes:30}")
    private long ttlMinutes;

    private CircuitBreaker circuitBreaker;

    @PostConstruct
    void init() {
        circuitBreaker = circuitBreakerRegistry.get(CIRCUIT_BREAKER_NAME,
                CircuitBreakerConfig.builder()
                        .failureThreshold(3)
                        .cooldownDuration(Duration.ofSeconds(30))
                        .halfOpenPermittedCalls(1)
                        .build());
    }

    // ── Auth Parameter operations ────────────────────────────────────────

    public Optional<String> getAuthParameter(String paramId) {
        return circuitBreaker.execute(
                () -> {
                    String key = AUTH_PARAM_KEY_PREFIX + paramId;
                    Object value = redisTemplate.opsForValue().get(key);
                    if (value == null) {
                        return Optional.<String>empty();
                    }
                    return Optional.of(objectMapper.convertValue(value, String.class));
                },
                Optional::empty
        );
    }

    public void putAuthParameter(String paramId, String paramValue) {
        circuitBreaker.executeVoid(() -> {
            String key = AUTH_PARAM_KEY_PREFIX + paramId;
            redisTemplate.opsForValue().set(key, paramValue, ttlMinutes, TimeUnit.MINUTES);
        });
    }

    public void evictAuthParameter(String paramId) {
        circuitBreaker.executeVoid(() -> {
            String key = AUTH_PARAM_KEY_PREFIX + paramId;
            redisTemplate.delete(key);
        });
    }

    // ── Security Config operations ───────────────────────────────────────

    public Optional<SecurityConfigDto> getSecurityConfig(String configId) {
        return circuitBreaker.execute(
                () -> {
                    String key = SECURITY_CONFIG_KEY_PREFIX + configId;
                    Object value = redisTemplate.opsForValue().get(key);
                    if (value == null) {
                        return Optional.<SecurityConfigDto>empty();
                    }
                    return Optional.of(objectMapper.convertValue(value, SecurityConfigDto.class));
                },
                Optional::empty
        );
    }

    public void putSecurityConfig(String configId, SecurityConfigDto securityConfigDto) {
        circuitBreaker.executeVoid(() -> {
            String key = SECURITY_CONFIG_KEY_PREFIX + configId;
            redisTemplate.opsForValue().set(key, securityConfigDto, ttlMinutes, TimeUnit.MINUTES);
        });
    }

    public void evictSecurityConfig(String configId) {
        circuitBreaker.executeVoid(() -> {
            String key = SECURITY_CONFIG_KEY_PREFIX + configId;
            redisTemplate.delete(key);
        });
    }

    // ── Observability ────────────────────────────────────────────────────

    public boolean isRedisAvailable() {
        return circuitBreaker.getState() != CircuitBreaker.State.OPEN;
    }
}
