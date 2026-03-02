package com.javaproject.application.service.impl;

import com.javaproject.application.model.AuthParameters;
import com.javaproject.application.repository.AuthParametersRepository;
import com.javaproject.application.repository.SecurityPolicyRepository;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthParameterService {

    private final Cache<String, String> authParameterLocalCache;
    private final RedisCacheService redisCacheService;
    private final AuthParametersRepository authParametersRepository;

    /**
     * Fetch auth parameter by policy name.
     * L1 (Local Caffeine) -> L2 (Redis) -> L3 (PostgreSQL DB)
     */
    public String getByPolicyName(String paramId) {
        // L1: Local cache lookup
        String cached = authParameterLocalCache.getIfPresent(paramId);
        if (cached != null) {
            log.debug("L1 cache HIT for policy [{}]", paramId);
            return cached;
        }
        log.debug("L1 cache MISS for policy [{}]", paramId);

        // L2: Redis cache lookup
        Optional<String> redisResult = redisCacheService.getAuthParameter(paramId);
        if (redisResult.isPresent()) {
            log.debug("L2 Redis cache HIT for policy [{}]", paramId);
            String paramValue = redisResult.get();
            authParameterLocalCache.put(paramId, paramValue);
            return paramValue;
        }
        log.debug("L2 Redis cache MISS for policy [{}]", paramId);

        // L3: Database lookup
        AuthParameters authParameters = authParametersRepository.findByParamId(paramId).orElseThrow(
                () -> new EntityNotFoundException("Auth parameter not found with paramId: " + paramId));

        // Populate both caches
        redisCacheService.putAuthParameter(paramId, authParameters.getParamValue());
        authParameterLocalCache.put(paramId, authParameters.getParamValue());
        log.debug("Loaded policy [{}] from DB and populated L1 + L2 caches", paramId);

        return authParameters.getParamValue();
    }

    /**
     * Evict a policy from all cache layers.
     * Call this when a security policy is updated in the DB.
     */
    public void evict(String policyName) {
        authParameterLocalCache.invalidate(policyName);
        redisCacheService.evictAuthParameter(policyName);
        log.info("Evicted policy [{}] from L1 + L2 caches", policyName);
    }

    /**
     * Evict all entries from the local cache.
     */
    public void evictAllLocal() {
        authParameterLocalCache.invalidateAll();
        log.info("Evicted all entries from L1 local cache");
    }


}
