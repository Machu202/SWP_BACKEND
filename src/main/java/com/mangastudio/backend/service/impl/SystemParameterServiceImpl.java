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
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SystemParameterServiceImpl implements SystemParameterService {

    private static final String MAX_PAGES_PER_CHAPTER = "MAX_PAGES_PER_CHAPTER";

    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "STRING", "INTEGER", "DECIMAL", "BOOLEAN", "JSON"
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
        if (MAX_PAGES_PER_CHAPTER.equals(key)) {
            if (!"INTEGER".equals(type)) {
                throw new RuntimeException("Error: MAX_PAGES_PER_CHAPTER must use the INTEGER type.");
            }
            try {
                if (Long.parseLong(normalized) < 1) {
                    throw new RuntimeException("Error: MAX_PAGES_PER_CHAPTER must be at least 1.");
                }
            } catch (NumberFormatException exception) {
                throw new RuntimeException("Error: MAX_PAGES_PER_CHAPTER must be a positive integer.");
            }
        }
        return normalized;
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
