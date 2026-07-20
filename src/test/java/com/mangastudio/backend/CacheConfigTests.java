package com.mangastudio.backend;

import com.mangastudio.backend.config.CacheConfig;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class CacheConfigTests {

    @Test
    void exposesBothSystemParameterCachesRequiredAtStartup() {
        CacheManager cacheManager = new CacheConfig().cacheManager();

        assertNotNull(cacheManager);
        assertNotNull(cacheManager.getCache("systemParameters"));
        assertNotNull(cacheManager.getCache("systemParameterList"));
    }
}
