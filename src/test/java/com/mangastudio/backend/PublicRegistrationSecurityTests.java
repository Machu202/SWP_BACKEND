package com.mangastudio.backend;

import com.mangastudio.backend.dto.request.RegisterRequest;
import com.mangastudio.backend.entity.Role;
import com.mangastudio.backend.entity.User;
import com.mangastudio.backend.repository.OtpRepository;
import com.mangastudio.backend.repository.RoleRepository;
import com.mangastudio.backend.repository.UserRepository;
import com.mangastudio.backend.security.JwtUtils;
import com.mangastudio.backend.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PublicRegistrationSecurityTests {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private PasswordEncoder encoder;
    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        encoder = mock(PasswordEncoder.class);
        service = new AuthServiceImpl(
                mock(AuthenticationManager.class),
                userRepository,
                roleRepository,
                encoder,
                mock(JwtUtils.class),
                mock(OtpRepository.class),
                mock(com.mangastudio.backend.repository.PasswordResetCodeRepository.class),
                mock(JavaMailSender.class)
        );
    }

    @Test
    void publicRegistrationRejectsAdminRoleVariants() {
        for (String role : new String[]{"Admin", "ROLE_ADMIN", "role_admin", " admin "}) {
            RegisterRequest request = request(role);
            assertThrows(AccessDeniedException.class, () -> service.registerUser(request), role);
        }

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(roleRepository);
    }

    @Test
    void publicRegistrationRejectsUnknownOrPrivilegedRole() {
        RegisterRequest request = request("Super Admin");

        RuntimeException error = assertThrows(RuntimeException.class, () -> service.registerUser(request));
        assertEquals(
                "Error: Public registration only supports Mangaka, Assistant, Tantou Editor, or Editorial Board.",
                error.getMessage()
        );
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void publicRegistrationAcceptsAndCanonicalizesAllowedRole() {
        RegisterRequest request = request("ROLE_TANTOU_EDITOR");
        Role tantou = Role.builder().id(3L).roleName("Tantou Editor").build();

        when(userRepository.existsByUsername("new-user")).thenReturn(false);
        when(userRepository.existsByEmail("new-user@example.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("0900000000")).thenReturn(false);
        when(roleRepository.findByRoleName("Tantou Editor")).thenReturn(Optional.of(tantou));
        when(encoder.encode("123456")).thenReturn("encoded-password");

        service.registerUser(request);

        verify(roleRepository).findByRoleName("Tantou Editor");
        verify(userRepository).save(argThat(user ->
                user.getRole() == tantou
                        && "encoded-password".equals(user.getPasswordHash())
                        && Boolean.TRUE.equals(user.getIsActive())
        ));
    }

    private RegisterRequest request(String role) {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("new-user");
        request.setEmail("new-user@example.com");
        request.setPhoneNumber("0900000000");
        request.setPassword("123456");
        request.setRole(role);
        return request;
    }
}
