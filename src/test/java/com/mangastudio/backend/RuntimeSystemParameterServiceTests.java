package com.mangastudio.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mangastudio.backend.entity.SystemParameter;
import com.mangastudio.backend.service.RuntimeSystemParameterService;
import com.mangastudio.backend.service.SystemParameterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RuntimeSystemParameterServiceTests {

    private SystemParameterService parameterService;
    private RuntimeSystemParameterService runtime;

    @BeforeEach
    void setUp() {
        parameterService = mock(SystemParameterService.class);
        runtime = new RuntimeSystemParameterService(parameterService, new ObjectMapper());
    }

    @Test
    void usesFallbackWhenAParameterHasNotBeenCreated() {
        when(parameterService.getParameterByKey("ENABLE_GOOGLE_LOGIN"))
                .thenThrow(new RuntimeException("Error: Parameter key not found: ENABLE_GOOGLE_LOGIN"));

        assertFalse(runtime.booleanValue("ENABLE_GOOGLE_LOGIN", false));
    }

    @Test
    void readsTypedIntegerAndIntegerArrayValues() {
        when(parameterService.getParameterByKey("MAX_CHAT_MESSAGE_LENGTH"))
                .thenReturn(parameter("MAX_CHAT_MESSAGE_LENGTH", "4500"));
        when(parameterService.getParameterByKey("DEADLINE_WARNING_DAYS"))
                .thenReturn(parameter("DEADLINE_WARNING_DAYS", "[1,3,7]"));

        assertEquals(4500, runtime.positiveInteger("MAX_CHAT_MESSAGE_LENGTH", 2000, 100_000));
        assertEquals(Set.of(1, 3, 7), runtime.integerArrayValue(
                "DEADLINE_WARNING_DAYS", Set.of(), 1, 365));
    }

    @Test
    void rejectsInvalidLegacyRowsInsteadOfCrashingWithAParseException() {
        when(parameterService.getParameterByKey("MAX_UPLOAD_MB"))
                .thenReturn(parameter("MAX_UPLOAD_MB", "ten"));

        RuntimeException error = assertThrows(RuntimeException.class, () ->
                runtime.positiveInteger("MAX_UPLOAD_MB", 10, 10));
        assertEquals("Error: System parameter MAX_UPLOAD_MB must be a positive integer from 1 to 10.",
                error.getMessage());
    }

    private SystemParameter parameter(String key, String value) {
        return SystemParameter.builder().paramKey(key).paramValue(value).build();
    }
}
