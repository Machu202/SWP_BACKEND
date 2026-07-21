package com.mangastudio.backend;

import com.mangastudio.backend.service.RuntimeSystemParameterService;
import com.mangastudio.backend.service.UploadPolicyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UploadPolicyServiceTests {

    private RuntimeSystemParameterService runtime;
    private UploadPolicyService policy;

    @BeforeEach
    void setUp() {
        runtime = mock(RuntimeSystemParameterService.class);
        policy = new UploadPolicyService(runtime);
        when(runtime.positiveInteger("MAX_REQUEST_MB", 50, 50)).thenReturn(50);
        when(runtime.stringArrayValue("ALLOWED_IMAGE_TYPES", Set.of("png", "jpg", "jpeg", "webp")))
                .thenReturn(Set.of("png", "jpg"));
    }

    @Test
    void blocksAFileThatExceedsTheAdminUploadLimit() {
        when(runtime.positiveInteger("MAX_UPLOAD_MB", 10, 10)).thenReturn(1);
        MockMultipartFile file = new MockMultipartFile(
                "file", "large.png", "image/png", new byte[1_048_577]);

        RuntimeException error = assertThrows(RuntimeException.class, () -> policy.validateFile(file));
        assertEquals("Error: The file exceeds the Admin upload limit of 1 MB.", error.getMessage());
    }

    @Test
    void enforcesAdminImageTypesWithoutRejectingAnAllowedPage() {
        when(runtime.positiveInteger("MAX_UPLOAD_MB", 10, 10)).thenReturn(10);
        MockMultipartFile allowed = new MockMultipartFile(
                "file", "page.png", "image/png", new byte[] {1});
        MockMultipartFile blocked = new MockMultipartFile(
                "file", "page.gif", "image/gif", new byte[] {1});

        assertDoesNotThrow(() -> policy.validatePageImage(allowed));
        assertThrows(RuntimeException.class, () -> policy.validatePageImage(blocked));
    }
}
