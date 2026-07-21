package com.mangastudio.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mangastudio.backend.entity.Role;
import com.mangastudio.backend.entity.SystemParameter;
import com.mangastudio.backend.entity.SystemParameterAudit;
import com.mangastudio.backend.entity.User;
import com.mangastudio.backend.repository.SystemParameterAuditRepository;
import com.mangastudio.backend.repository.SystemParameterRepository;
import com.mangastudio.backend.repository.UserRepository;
import com.mangastudio.backend.service.impl.SystemParameterServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SystemParameterHardeningTests {

    private SystemParameterRepository parameterRepository;
    private SystemParameterAuditRepository auditRepository;
    private UserRepository userRepository;
    private SystemParameterServiceImpl service;
    private User admin;

    @BeforeEach
    void setUp() {
        parameterRepository = mock(SystemParameterRepository.class);
        auditRepository = mock(SystemParameterAuditRepository.class);
        userRepository = mock(UserRepository.class);
        service = new SystemParameterServiceImpl(
                parameterRepository, auditRepository, userRepository, new ObjectMapper());
        admin = User.builder()
                .id(1L)
                .username("admin")
                .fullName("System Admin")
                .role(Role.builder().id(1L).roleName("Admin").build())
                .isActive(true)
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(auditRepository.save(any(SystemParameterAudit.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsTypedIntegerAndWritesAuditMetadata() {
        when(parameterRepository.findByParamKeyIgnoreCase("DEFAULT_PAGINATION_SIZE"))
                .thenReturn(Optional.empty());
        when(parameterRepository.save(any(SystemParameter.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SystemParameter saved = service.createParameter(
                "default_pagination_size", "20", "integer", 1L);

        assertEquals("DEFAULT_PAGINATION_SIZE", saved.getParamKey());
        assertEquals("INTEGER", saved.getParamType());
        assertEquals("20", saved.getParamValue());
        assertEquals(1L, saved.getUpdatedBy());
        assertEquals("System Admin", saved.getUpdatedByName());
        assertNotNull(saved.getUpdatedAt());
        verify(auditRepository).save(argThat(audit ->
                "CREATE".equals(audit.getAction())
                        && "20".equals(audit.getNewValue())
                        && audit.getChangedBy().equals(1L)));
    }

    @Test
    void rejectsInvalidIntegerBeforeSaving() {
        when(parameterRepository.findByParamKeyIgnoreCase("DEFAULT_PAGINATION_SIZE"))
                .thenReturn(Optional.empty());

        RuntimeException error = assertThrows(RuntimeException.class, () ->
                service.createParameter("DEFAULT_PAGINATION_SIZE", "twenty", "INTEGER", 1L));

        assertTrue(error.getMessage().contains("not valid for type INTEGER"));
        verify(parameterRepository, never()).save(any());
        verify(auditRepository, never()).save(any());
    }

    @Test
    void acceptsPositiveMaxPagesPerChapterLimit() {
        when(parameterRepository.findByParamKeyIgnoreCase("MAX_PAGES_PER_CHAPTER"))
                .thenReturn(Optional.empty());
        when(parameterRepository.save(any(SystemParameter.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SystemParameter saved = service.createParameter(
                "MAX_PAGES_PER_CHAPTER", "80", "INTEGER", 1L);

        assertEquals("80", saved.getParamValue());
        assertEquals("INTEGER", saved.getParamType());
    }

    @Test
    void rejectsNonIntegerMaxPagesPerChapterLimit() {
        RuntimeException error = assertThrows(RuntimeException.class, () ->
                service.createParameter("MAX_PAGES_PER_CHAPTER", "80", "STRING", 1L));

        assertEquals("Error: MAX_PAGES_PER_CHAPTER must use the INTEGER type.", error.getMessage());
        verify(parameterRepository, never()).save(any());
    }

    @Test
    void rejectsNonPositiveMaxPagesPerChapterLimit() {
        RuntimeException error = assertThrows(RuntimeException.class, () ->
                service.createParameter("MAX_PAGES_PER_CHAPTER", "0", "INTEGER", 1L));

        assertEquals("Error: MAX_PAGES_PER_CHAPTER must be between 1 and 10000.", error.getMessage());
        verify(parameterRepository, never()).save(any());
    }

    @Test
    void rejectsKnownParameterWithTheWrongType() {
        RuntimeException error = assertThrows(RuntimeException.class, () ->
                service.createParameter("ENABLE_GOOGLE_LOGIN", "true", "STRING", 1L));

        assertEquals("Error: ENABLE_GOOGLE_LOGIN must use the BOOLEAN type.", error.getMessage());
        verify(parameterRepository, never()).save(any());
    }

    @Test
    void rejectsDeploymentSecretsFromTheBusinessSettingsTable() {
        RuntimeException error = assertThrows(RuntimeException.class, () ->
                service.createParameter("JWT_SECRET", "do-not-store-this", "STRING", 1L));

        assertTrue(error.getMessage().contains("credentials and secrets cannot be stored"));
        verify(parameterRepository, never()).save(any());
    }

    @Test
    void rejectsApprovalRatioOutsideZeroToOne() {
        RuntimeException error = assertThrows(RuntimeException.class, () ->
                service.createParameter("BOARD_APPROVAL_RATIO", "1.1", "DECIMAL", 1L));

        assertTrue(error.getMessage().contains("no greater than 1"));
        verify(parameterRepository, never()).save(any());
    }

    @Test
    void rejectsInvalidDeadlineWarningDays() {
        RuntimeException error = assertThrows(RuntimeException.class, () ->
                service.createParameter("DEADLINE_WARNING_DAYS", "[0,366]", "JSON", 1L));

        assertTrue(error.getMessage().contains("integers from 1 to 365"));
        verify(parameterRepository, never()).save(any());
    }

    @Test
    void rejectsNonAdminEvenWhenServiceIsCalledDirectly() {
        User mangaka = User.builder()
                .id(2L)
                .username("mangaka")
                .role(Role.builder().id(2L).roleName("Mangaka").build())
                .isActive(true)
                .build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(mangaka));

        assertThrows(org.springframework.security.access.AccessDeniedException.class, () ->
                service.createParameter("MAX_FILE_SIZE", "10", "INTEGER", 2L));
        verify(parameterRepository, never()).save(any());
    }
}
