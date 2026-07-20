package com.mangastudio.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot 4 uses its newer Jackson stack for HTTP auto-configuration, while
 * existing project code and jjwt-jackson still use the Jackson 2
 * com.fasterxml type. SystemParameterService needs only readTree validation,
 * so expose one explicit compatibility bean without replacing an application-
 * provided mapper when one already exists.
 */
@Configuration(proxyBeanMethods = false)
public class JacksonCompatibilityConfig {

    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper legacyObjectMapper() {
        return new ObjectMapper();
    }
}
