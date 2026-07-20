package com.mangastudio.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mangastudio.backend.config.JacksonCompatibilityConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JacksonCompatibilityConfigTests {

    @Test
    void exposesJacksonTwoMapperRequiredBySystemParameters() throws Exception {
        ObjectMapper mapper = new JacksonCompatibilityConfig().legacyObjectMapper();

        assertNotNull(mapper);
        assertEquals(20, mapper.readTree("{\"pagination\":20}").get("pagination").asInt());
    }
}
