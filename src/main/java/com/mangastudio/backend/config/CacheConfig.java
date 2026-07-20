package com.mangastudio.backend.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot 4 does not create a cache provider merely because
 * {@code @EnableCaching} is present. Provide a small in-process cache for the
 * low-volume System Parameters table while allowing deployments to replace it
 * later with Redis or another CacheManager.
 */
@Configuration(proxyBeanMethods = false)
public class CacheConfig {

    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("systemParameters", "systemParameterList");
    }
}
