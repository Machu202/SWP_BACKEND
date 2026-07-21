package com.mangastudio.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UploadPolicyService {

    private static final long BYTES_PER_MEGABYTE = 1024L * 1024L;
    private static final Set<String> DEFAULT_IMAGE_TYPES = Set.of("png", "jpg", "jpeg", "webp");

    private final RuntimeSystemParameterService runtimeParameters;

    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Error: Select a non-empty file to upload.");
        }
        int perFileMb = runtimeParameters.positiveInteger("MAX_UPLOAD_MB", 10, 10);
        int perRequestMb = runtimeParameters.positiveInteger("MAX_REQUEST_MB", 50, 50);
        long effectiveLimitBytes = Math.min(perFileMb, perRequestMb) * BYTES_PER_MEGABYTE;
        if (file.getSize() > effectiveLimitBytes) {
            throw new RuntimeException("Error: The file exceeds the Admin upload limit of "
                    + Math.min(perFileMb, perRequestMb) + " MB.");
        }
    }

    public void validatePageImage(MultipartFile file) {
        validateFile(file);
        Set<String> allowed = runtimeParameters.stringArrayValue("ALLOWED_IMAGE_TYPES", DEFAULT_IMAGE_TYPES);
        String extension = extension(file.getOriginalFilename());
        String contentSubtype = contentSubtype(file.getContentType());
        if ((!extension.isEmpty() && allowed.contains(extension))
                || (!contentSubtype.isEmpty() && allowed.contains(contentSubtype))) {
            return;
        }
        throw new RuntimeException("Error: Page images must use one of these file types: "
                + String.join(", ", allowed) + ".");
    }

    private String extension(String filename) {
        String value = filename == null ? "" : filename.trim().toLowerCase(Locale.ROOT);
        int dot = value.lastIndexOf('.');
        return dot >= 0 && dot < value.length() - 1 ? value.substring(dot + 1) : "";
    }

    private String contentSubtype(String contentType) {
        String value = contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
        if (!value.startsWith("image/") || value.length() <= 6) return "";
        String subtype = value.substring(6);
        return "pjpeg".equals(subtype) ? "jpeg" : subtype;
    }
}
