package com.mangastudio.backend.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mangastudio.backend.entity.SystemParameter;
import com.mangastudio.backend.entity.SystemParameterAudit;
import com.mangastudio.backend.entity.User;
import com.mangastudio.backend.repository.SystemParameterAuditRepository;
import com.mangastudio.backend.repository.SystemParameterRepository;
import com.mangastudio.backend.repository.UserRepository;
import com.mangastudio.backend.service.SystemParameterService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SystemParameterServiceImpl implements SystemParameterService {

    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "STRING", "INTEGER", "DECIMAL", "BOOLEAN", "JSON"
    );
    private static final Map<String, String> KNOWN_PARAMETER_TYPES = Map.ofEntries(
            Map.entry("MAX_UPLOAD_MB", "INTEGER"),
            Map.entry("MAX_REQUEST_MB", "INTEGER"),
            Map.entry("MAX_PAGES_PER_CHAPTER", "INTEGER"),
            Map.entry("MAX_CHAT_MESSAGE_LENGTH", "INTEGER"),
            Map.entry("REVIEW_TIMEOUT_HOURS", "INTEGER"),
            Map.entry("DEADLINE_SCAN_SECONDS", "INTEGER"),
            Map.entry("PUBLICATION_SCAN_SECONDS", "INTEGER"),
            Map.entry("TELEMETRY_FLUSH_SECONDS", "INTEGER"),
            Map.entry("ENABLE_PUBLIC_REGISTRATION", "BOOLEAN"),
            Map.entry("ENABLE_GOOGLE_LOGIN", "BOOLEAN"),
            Map.entry("ENABLE_EMAIL_OTP", "BOOLEAN"),
            Map.entry("DEFAULT_SERIES_STATUS", "STRING"),
            Map.entry("DEFAULT_CHAPTER_STATUS", "STRING"),
            Map.entry("BOARD_APPROVAL_RATIO", "DECIMAL"),
            Map.entry("DEADLINE_WARNING_DAYS", "JSON"),
            Map.entry("ALLOWED_IMAGE_TYPES", "JSON")
    );
    private static final Map<String, Long> POSITIVE_INTEGER_MAXIMUMS = Map.ofEntries(
            Map.entry("MAX_UPLOAD_MB", 10L),
            Map.entry("MAX_REQUEST_MB", 50L),
            Map.entry("MAX_PAGES_PER_CHAPTER", 10_000L),
            Map.entry("MAX_CHAT_MESSAGE_LENGTH", 100_000L),
            Map.entry("REVIEW_TIMEOUT_HOURS", 8_760L),
            Map.entry("DEADLINE_SCAN_SECONDS", 86_400L),
            Map.entry("PUBLICATION_SCAN_SECONDS", 86_400L),
            Map.entry("TELEMETRY_FLUSH_SECONDS", 86_400L)
    );
    private static final Set<String> FORBIDDEN_SENSITIVE_KEYS = Set.of(
            "DATABASE_PASSWORD", "DATABASE_URL", "DATABASE_USERNAME",
            "JWT_SECRET", "CLOUDINARY_API_KEY", "CLOUDINARY_API_SECRET",
            "EMAIL_PASSWORD", "MAIL_PASSWORD", "CORS_ORIGIN", "SERVER_PORT"
    );

    private final SystemParameterRepository parameterRepository;
    private final SystemParameterAuditRepository auditRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "systemParameterList", key = "'all'")
    public List<SystemParameter> getAllParameters() {
        return parameterRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "systemParameters", key = "#key.toUpperCase()")
    public SystemParameter getParameterByKey(String key) {
        String normalizedKey = normalizeKey(key);
        return parameterRepository.findByParamKeyIgnoreCase(normalizedKey)
                .orElseThrow(() -> new RuntimeException("Error: Parameter key not found: " + normalizedKey));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "systemParameters", allEntries = true),
            @CacheEvict(cacheNames = "systemParameterList", allEntries = true)
    })
    public SystemParameter createParameter(String key, String value, String type, Long currentUserId) {
        User admin = requireAdmin(currentUserId);
        String normalizedKey = normalizeKey(key);
        validateAllowedKey(normalizedKey);
        String normalizedType = normalizeType(type, "STRING");
        String normalizedValue = validateValue(normalizedKey, value, normalizedType);

        if (parameterRepository.findByParamKeyIgnoreCase(normalizedKey).isPresent()) {
            throw new RuntimeException("Error: Parameter key already exists: " + normalizedKey);
        }

        LocalDateTime changedAt = LocalDateTime.now();
        SystemParameter saved = parameterRepository.save(SystemParameter.builder()
                .paramKey(normalizedKey)
                .paramValue(normalizedValue)
                .paramType(normalizedType)
                .updatedBy(admin.getId())
                .updatedByName(displayName(admin))
                .updatedAt(changedAt)
                .build());
        saveAudit(saved.getParamKey(), saved.getParamType(), "CREATE", null,
                saved.getParamValue(), admin, changedAt);
        return saved;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "systemParameters", allEntries = true),
            @CacheEvict(cacheNames = "systemParameterList", allEntries = true)
    })
    public SystemParameter updateParameter(String key, String value, String type, Long currentUserId) {
        User admin = requireAdmin(currentUserId);
        String normalizedKey = normalizeKey(key);
        validateAllowedKey(normalizedKey);
        SystemParameter parameter = parameterRepository.findByParamKeyIgnoreCase(normalizedKey)
                .orElseThrow(() -> new RuntimeException("Error: Parameter key not found: " + normalizedKey));

        String normalizedType = normalizeType(type, parameter.getParamType());
        String normalizedValue = validateValue(normalizedKey, value, normalizedType);
        String oldValue = parameter.getParamValue();
        LocalDateTime changedAt = LocalDateTime.now();

        parameter.setParamValue(normalizedValue);
        parameter.setParamType(normalizedType);
        parameter.setUpdatedBy(admin.getId());
        parameter.setUpdatedByName(displayName(admin));
        parameter.setUpdatedAt(changedAt);
        SystemParameter saved = parameterRepository.save(parameter);
        saveAudit(saved.getParamKey(), saved.getParamType(), "UPDATE", oldValue,
                saved.getParamValue(), admin, changedAt);
        return saved;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "systemParameters", allEntries = true),
            @CacheEvict(cacheNames = "systemParameterList", allEntries = true)
    })
    public void deleteParameter(String key, Long currentUserId) {
        User admin = requireAdmin(currentUserId);
        String normalizedKey = normalizeKey(key);
        SystemParameter parameter = parameterRepository.findByParamKeyIgnoreCase(normalizedKey)
                .orElseThrow(() -> new RuntimeException("Error: Parameter key not found: " + normalizedKey));

        LocalDateTime changedAt = LocalDateTime.now();
        saveAudit(parameter.getParamKey(), normalizeType(parameter.getParamType(), "STRING"), "DELETE",
                parameter.getParamValue(), null, admin, changedAt);
        parameterRepository.delete(parameter);
    }

    private User requireAdmin(Long currentUserId) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Error: Admin user not found"));
        String role = user.getRole() != null ? user.getRole().getRoleName() : "";
        if (!"ADMIN".equals(role == null ? "" : role.trim().toUpperCase(Locale.ROOT))) {
            throw new AccessDeniedException("Only Admin can change system parameters.");
        }
        return user;
    }

    private String normalizeKey(String key) {
        String normalized = key == null ? "" : key.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        if (!normalized.matches("[A-Z][A-Z0-9_]{0,99}")) {
            throw new RuntimeException("Error: Parameter key must use A-Z, 0-9 and underscores only.");
        }
        return normalized;
    }

    private String normalizeType(String type, String fallback) {
        String normalized = type == null || type.isBlank() ? fallback : type;
        normalized = normalized == null || normalized.isBlank()
                ? "STRING"
                : normalized.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_TYPES.contains(normalized)) {
            throw new RuntimeException("Error: Unsupported parameter type: " + normalized);
        }
        return normalized;
    }

    private String validateValue(String key, String value, String type) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            throw new RuntimeException("Error: Parameter value is required.");
        }
        if (normalized.length() > 255) {
            throw new RuntimeException("Error: Parameter value cannot exceed 255 characters.");
        }
        try {
            switch (type) {
                case "INTEGER" -> Long.parseLong(normalized);
                case "DECIMAL" -> new BigDecimal(normalized);
                case "BOOLEAN" -> {
                    if (!"true".equalsIgnoreCase(normalized) && !"false".equalsIgnoreCase(normalized)) {
                        throw new IllegalArgumentException("not a boolean");
                    }
                    normalized = normalized.toLowerCase(Locale.ROOT);
                }
                case "JSON" -> objectMapper.readTree(normalized);
                default -> { }
            }
        } catch (Exception exception) {
            throw new RuntimeException("Error: Value '" + normalized + "' is not valid for type " + type + ".");
        }
        validateKnownParameter(key, normalized, type);
        return normalized;
    }

    private void validateAllowedKey(String key) {
        if (FORBIDDEN_SENSITIVE_KEYS.contains(key)
                || key.startsWith("DATASOURCE_")
                || key.startsWith("CLOUDINARY_SECRET_")
                || key.endsWith("_PASSWORD")
                || key.endsWith("_SECRET")) {
            throw new RuntimeException("Error: Deployment credentials and secrets cannot be stored in System Parameters.");
        }
    }

    private void validateKnownParameter(String key, String value, String type) {
        String expectedType = KNOWN_PARAMETER_TYPES.get(key);
        if (expectedType != null && !expectedType.equals(type)) {
            throw new RuntimeException("Error: " + key + " must use the " + expectedType + " type.");
        }

        Long maximum = POSITIVE_INTEGER_MAXIMUMS.get(key);
        if (maximum != null) {
            long parsed = Long.parseLong(value);
            if (parsed < 1 || parsed > maximum) {
                throw new RuntimeException("Error: " + key + " must be between 1 and " + maximum + ".");
            }
        }

        if ("BOARD_APPROVAL_RATIO".equals(key)) {
            BigDecimal ratio = new BigDecimal(value);
            if (ratio.compareTo(BigDecimal.ZERO) <= 0 || ratio.compareTo(BigDecimal.ONE) > 0) {
                throw new RuntimeException("Error: BOARD_APPROVAL_RATIO must be greater than 0 and no greater than 1.");
            }
        }

        if ("DEFAULT_SERIES_STATUS".equals(key) && !"DRAFT".equalsIgnoreCase(value)) {
            throw new RuntimeException("Error: DEFAULT_SERIES_STATUS must remain DRAFT to preserve the approval workflow.");
        }
        if ("DEFAULT_CHAPTER_STATUS".equals(key) && !"DRAFT".equalsIgnoreCase(value)) {
            throw new RuntimeException("Error: DEFAULT_CHAPTER_STATUS must remain DRAFT to preserve the review workflow.");
        }

        if ("ALLOWED_IMAGE_TYPES".equals(key)) {
            try {
                var root = objectMapper.readTree(value);
                if (!root.isArray() || root.isEmpty()) throw new IllegalArgumentException("empty array");
                for (var node : root) {
                    if (!node.isTextual() || !node.asText().matches("[A-Za-z0-9]+")) {
                        throw new IllegalArgumentException("invalid image type");
                    }
                }
            } catch (Exception exception) {
                throw new RuntimeException("Error: ALLOWED_IMAGE_TYPES must be a non-empty JSON array of file extensions.");
            }
        }

        if ("DEADLINE_WARNING_DAYS".equals(key)) {
            try {
                var root = objectMapper.readTree(value);
                if (!root.isArray() || root.isEmpty()) throw new IllegalArgumentException("empty array");
                for (var node : root) {
                    if (!node.canConvertToInt() || node.asInt() < 1 || node.asInt() > 365) {
                        throw new IllegalArgumentException("invalid warning day");
                    }
                }
            } catch (Exception exception) {
                throw new RuntimeException("Error: DEADLINE_WARNING_DAYS must be a JSON array of integers from 1 to 365.");
            }
        }
    }

    private void saveAudit(String key, String type, String action, String oldValue, String newValue,
                           User admin, LocalDateTime changedAt) {
        auditRepository.save(SystemParameterAudit.builder()
                .paramKey(key)
                .paramType(type)
                .action(action)
                .oldValue(oldValue)
                .newValue(newValue)
                .changedBy(admin.getId())
                .changedByName(displayName(admin))
                .changedAt(changedAt)
                .build());
    }

    private String displayName(User user) {
        if (user.getFullName() != null && !user.getFullName().isBlank()) return user.getFullName().trim();
        if (user.getUsername() != null && !user.getUsername().isBlank()) return user.getUsername().trim();
        return "Admin #" + user.getId();
    }
}
