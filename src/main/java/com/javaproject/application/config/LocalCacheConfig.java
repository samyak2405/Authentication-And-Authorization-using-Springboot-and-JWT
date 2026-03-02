package com.javaproject.application.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.javaproject.application.dto.SecurityConfigDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class LocalCacheConfig {

    @Value("${cache.local.ttl-minutes:10}")
    private long ttlMinutes;

    @Value("${cache.local.max-size:100}")
    private long maxSize;

    @Bean
    public Cache<String, String> authParameterLocalCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(ttlMinutes, TimeUnit.MINUTES)
                .maximumSize(maxSize)
                .build();
    }

    @Bean Cache<String, SecurityConfigDto> securityConfigLocalCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(ttlMinutes, TimeUnit.MINUTES)
                .maximumSize(maxSize)
                .build();
    }
}
