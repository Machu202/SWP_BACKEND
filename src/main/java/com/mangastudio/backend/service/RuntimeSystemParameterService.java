package com.mangastudio.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mangastudio.backend.entity.SystemParameter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/** Provides typed, cached access to business-safe runtime settings. */
@Service
@RequiredArgsConstructor
public class RuntimeSystemParameterService {

    private final SystemParameterService systemParameterService;
    private final ObjectMapper objectMapper;

    public OptionalInt optionalPositiveInteger(String key, int maximum) {
        Optional<SystemParameter> parameter = findParameter(key);
        if (parameter.isEmpty()) return OptionalInt.empty();
        try {
            int value = Integer.parseInt(parameter.get().getParamValue().trim());
            if (value < 1 || value > maximum) throw new NumberFormatException("outside supported range");
            return OptionalInt.of(value);
        } catch (RuntimeException exception) {
            throw invalidRuntimeValue(key, "a positive integer from 1 to " + maximum, exception);
        }
    }

    public int positiveInteger(String key, int fallback, int maximum) {
        return optionalPositiveInteger(key, maximum).orElse(fallback);
    }

    public boolean booleanValue(String key, boolean fallback) {
        Optional<SystemParameter> parameter = findParameter(key);
        if (parameter.isEmpty()) return fallback;
        String value = parameter.get().getParamValue() == null
                ? ""
                : parameter.get().getParamValue().trim().toLowerCase(Locale.ROOT);
        if ("true".equals(value)) return true;
        if ("false".equals(value)) return false;
        throw invalidRuntimeValue(key, "true or false", null);
    }

    public Optional<BigDecimal> optionalDecimal(String key) {
        Optional<SystemParameter> parameter = findParameter(key);
        if (parameter.isEmpty()) return Optional.empty();
        try {
            return Optional.of(new BigDecimal(parameter.get().getParamValue().trim()));
        } catch (RuntimeException exception) {
            throw invalidRuntimeValue(key, "a decimal number", exception);
        }
    }

    public String stringValue(String key, String fallback) {
        return findParameter(key)
                .map(SystemParameter::getParamValue)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .orElse(fallback);
    }

    public Set<String> stringArrayValue(String key, Set<String> fallback) {
        Optional<SystemParameter> parameter = findParameter(key);
        if (parameter.isEmpty()) return Set.copyOf(fallback);
        try {
            JsonNode root = objectMapper.readTree(parameter.get().getParamValue());
            if (!root.isArray()) throw new IllegalArgumentException("not an array");
            Set<String> values = new LinkedHashSet<>();
            for (JsonNode node : root) {
                if (!node.isTextual() || node.asText().isBlank()) {
                    throw new IllegalArgumentException("array contains a non-text value");
                }
                values.add(node.asText().trim().toLowerCase(Locale.ROOT));
            }
            if (values.isEmpty()) throw new IllegalArgumentException("array is empty");
            return Set.copyOf(values);
        } catch (Exception exception) {
            throw invalidRuntimeValue(key, "a non-empty JSON array of strings", exception);
        }
    }

    public Set<Integer> integerArrayValue(String key, Set<Integer> fallback, int minimum, int maximum) {
        Optional<SystemParameter> parameter = findParameter(key);
        if (parameter.isEmpty()) return Set.copyOf(fallback);
        try {
            JsonNode root = objectMapper.readTree(parameter.get().getParamValue());
            if (!root.isArray()) throw new IllegalArgumentException("not an array");
            Set<Integer> values = new LinkedHashSet<>();
            for (JsonNode node : root) {
                if (!node.canConvertToInt()) throw new IllegalArgumentException("array contains a non-integer value");
                int value = node.asInt();
                if (value < minimum || value > maximum) throw new IllegalArgumentException("outside supported range");
                values.add(value);
            }
            if (values.isEmpty()) throw new IllegalArgumentException("array is empty");
            return Set.copyOf(values);
        } catch (Exception exception) {
            throw invalidRuntimeValue(key,
                    "a non-empty JSON array of integers from " + minimum + " to " + maximum,
                    exception);
        }
    }

    private Optional<SystemParameter> findParameter(String key) {
        try {
            return Optional.of(systemParameterService.getParameterByKey(key));
        } catch (RuntimeException exception) {
            if (exception.getMessage() != null && exception.getMessage().startsWith("Error: Parameter key not found:")) {
                return Optional.empty();
            }
            throw exception;
        }
    }

    private RuntimeException invalidRuntimeValue(String key, String expected, Throwable cause) {
        String message = "Error: System parameter " + key + " must be " + expected + ".";
        return cause == null ? new RuntimeException(message) : new RuntimeException(message, cause);
    }
}
