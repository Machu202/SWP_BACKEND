package com.mangastudio.backend;

import com.mangastudio.backend.dto.request.RequestOtpRequest;
import com.mangastudio.backend.dto.request.ResetPasswordRequest;
import com.mangastudio.backend.entity.PasswordResetCode;
import com.mangastudio.backend.entity.User;
import com.mangastudio.backend.repository.OtpRepository;
import com.mangastudio.backend.repository.PasswordResetCodeRepository;
import com.mangastudio.backend.repository.RoleRepository;
import com.mangastudio.backend.repository.UserRepository;
import com.mangastudio.backend.security.JwtUtils;
import com.mangastudio.backend.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordResetServiceTests {

    private UserRepository userRepository;
    private PasswordResetCodeRepository resetCodeRepository;
    private PasswordEncoder passwordEncoder;
    private JavaMailSender mailSender;
    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        resetCodeRepository = mock(PasswordResetCodeRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        mailSender = mock(JavaMailSender.class);
        service = new AuthServiceImpl(
                mock(AuthenticationManager.class),
                userRepository,
                mock(RoleRepository.class),
                passwordEncoder,
                mock(JwtUtils.class),
                mock(OtpRepository.class),
                resetCodeRepository,
                mailSender);
    }

    @Test
    void requestCreatesASeparateOneTimeResetCodeAndEmailsIt() {
        User user = User.builder().id(5L).username("mika").email("Mika@example.test")
                .passwordHash("old-hash").isActive(true).build();
        when(userRepository.findByEmailIgnoreCase("mika@example.test")).thenReturn(Optional.of(user));
        when(resetCodeRepository.save(any(PasswordResetCode.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.requestPasswordReset(new RequestOtpRequest(" mika@example.test "));

        assertTrue(response.getMessage().startsWith("If an active account"));
        verify(resetCodeRepository).deleteByEmail("Mika@example.test");
        ArgumentCaptor<PasswordResetCode> codeCaptor = ArgumentCaptor.forClass(PasswordResetCode.class);
        verify(resetCodeRepository).save(codeCaptor.capture());
        assertTrue(codeCaptor.getValue().getCode().matches("\\d{6}"));
        assertTrue(codeCaptor.getValue().getExpirationTime().isAfter(LocalDateTime.now().plusMinutes(9)));
        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mailCaptor.capture());
        assertTrue(mailCaptor.getValue().getText().contains(codeCaptor.getValue().getCode()));
    }

    @Test
    void successfulResetHashesThePasswordConsumesTheCodeAndRevokesOldSession() {
        User user = User.builder().id(6L).username("mika").email("mika@example.test")
                .passwordHash("old-hash").activeSessionId("old-session").isActive(true).build();
        PasswordResetCode code = PasswordResetCode.builder().id(2L).email(user.getEmail())
                .code("123456").expirationTime(LocalDateTime.now().plusMinutes(5)).build();
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail(user.getEmail());
        request.setOtpCode("123456");
        request.setNewPassword("new-password");
        when(userRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(resetCodeRepository.findByEmailAndCode(user.getEmail(), "123456")).thenReturn(Optional.of(code));
        when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

        var response = service.resetPassword(request);

        assertEquals("Password reset successfully. You can now log in.", response.getMessage());
        assertEquals("new-hash", user.getPasswordHash());
        assertNull(user.getActiveSessionId());
        verify(userRepository).saveAndFlush(user);
        verify(resetCodeRepository).delete(code);
    }

    @Test
    void unknownEmailReturnsTheSameGenericRequestResponseWithoutSendingMail() {
        when(userRepository.findByEmailIgnoreCase("missing@example.test")).thenReturn(Optional.empty());

        var response = service.requestPasswordReset(new RequestOtpRequest("missing@example.test"));

        assertTrue(response.getMessage().startsWith("If an active account"));
        verify(resetCodeRepository, never()).save(any());
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }
}
