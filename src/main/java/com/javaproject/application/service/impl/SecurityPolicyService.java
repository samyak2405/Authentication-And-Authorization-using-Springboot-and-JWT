package com.javaproject.application.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.javaproject.application.dto.SecurityConfigDto;
import com.javaproject.application.mapper.Mapper;
import com.javaproject.application.model.SecurityPolicy;
import com.javaproject.application.repository.SecurityPolicyRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityPolicyService {

    private final Cache<String, SecurityConfigDto> securityConfigCache;
    private final RedisCacheService redisCacheService;
    private final SecurityPolicyRepository securityPolicyRepository;

    public SecurityConfigDto getByConfigId(String configId) {
        // L1: Local cache lookup
        SecurityConfigDto cached = securityConfigCache.getIfPresent(configId);
        if (cached != null) {
            log.debug("L1 cache HIT for config [{}]", configId);
            return cached;
        }
        log.debug("L1 cache MISS for config [{}]", configId);

        // L2: Redis cache lookup
        return redisCacheService.getSecurityConfig(configId)
                .map(config -> {
                    log.debug("L2 Redis cache HIT for config [{}]", configId);
                    securityConfigCache.put(configId, config);
                    return config;
                })
                .orElseGet(() -> {
                    log.debug("L2 Redis cache MISS for config [{}]", configId);
                    // L3: Database lookup
                    SecurityPolicy dbConfig = securityPolicyRepository.findByName(configId)
                            .orElseThrow(() -> new EntityNotFoundException("Security config not found with id: " + configId));

                    SecurityConfigDto securityConfigDto = Mapper.toDto(dbConfig);
                    // Populate both caches
                    redisCacheService.putSecurityConfig(configId, securityConfigDto);
                    securityConfigCache.put(configId, securityConfigDto);
                    log.debug("Loaded config [{}] from DB and populated L1 + L2 caches", configId);
                    return securityConfigDto;
                });
    }

    /**
     * Evict a policy from all cache layers.
     * Call this when a security policy is updated in the DB.
     */
    public void evict(String configId) {
        securityConfigCache.invalidate(configId);
        redisCacheService.evictSecurityConfig(configId);
        log.info("Evicted policy [{}] from L1 + L2 caches", configId);
    }

    /**
     * Evict all entries from the local cache.
     */
    public void evictAllLocal() {
        securityConfigCache.invalidateAll();
        log.info("Evicted all entries from L1 local cache");
    }
}
