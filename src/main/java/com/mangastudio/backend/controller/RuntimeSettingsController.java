package com.mangastudio.backend.controller;

import com.mangastudio.backend.service.RuntimeSystemParameterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** Exposes only non-sensitive limits and feature flags needed by public UI controls. */
@RestController
@RequestMapping("/api/v1/runtime-settings")
@RequiredArgsConstructor
public class RuntimeSettingsController {

    private final RuntimeSystemParameterService runtimeParameters;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getRuntimeSettings() {
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("maxUploadMb", runtimeParameters.positiveInteger("MAX_UPLOAD_MB", 10, 10));
        settings.put("maxRequestMb", runtimeParameters.positiveInteger("MAX_REQUEST_MB", 50, 50));
        settings.put("maxPagesPerChapter", runtimeParameters.positiveInteger("MAX_PAGES_PER_CHAPTER", 1000, 10_000));
        settings.put("maxChatMessageLength", runtimeParameters.positiveInteger("MAX_CHAT_MESSAGE_LENGTH", 2000, 100_000));
        settings.put("enablePublicRegistration", runtimeParameters.booleanValue("ENABLE_PUBLIC_REGISTRATION", true));
        settings.put("enableGoogleLogin", runtimeParameters.booleanValue("ENABLE_GOOGLE_LOGIN", true));
        settings.put("enableEmailOtp", runtimeParameters.booleanValue("ENABLE_EMAIL_OTP", true));
        return ResponseEntity.ok(settings);
    }
}
