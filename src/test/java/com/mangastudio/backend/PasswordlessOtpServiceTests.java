package com.mangastudio.backend;

import com.mangastudio.backend.dto.request.RequestOtpRequest;
import com.mangastudio.backend.entity.OtpCode;
import com.mangastudio.backend.entity.User;
import com.mangastudio.backend.repository.OtpRepository;
import com.mangastudio.backend.repository.RoleRepository;
import com.mangastudio.backend.repository.UserRepository;
import com.mangastudio.backend.security.JwtUtils;
import com.mangastudio.backend.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PasswordlessOtpServiceTests {

    private AuthenticationManager authenticationManager;
    private UserRepository userRepository;
    private OtpRepository otpRepository;
    private JavaMailSender mailSender;
    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        authenticationManager = mock(AuthenticationManager.class);
        userRepository = mock(UserRepository.class);
        otpRepository = mock(OtpRepository.class);
        mailSender = mock(JavaMailSender.class);
        service = new AuthServiceImpl(
                authenticationManager,
                userRepository,
                mock(RoleRepository.class),
                mock(PasswordEncoder.class),
                mock(JwtUtils.class),
                otpRepository,
                mailSender
        );
    }

    @Test
    void requestingOtpNeedsOnlyTheEmailAndNeverAuthenticatesAPassword() {
        User user = User.builder()
                .id(7L)
                .username("mika")
                .email("Mika@Example.test")
                .passwordHash("existing-password-hash")
                .isActive(true)
                .build();
        when(userRepository.findByEmailIgnoreCase("mika@example.test")).thenReturn(Optional.of(user));

        var response = service.generateOtpForEmail(new RequestOtpRequest("  mika@example.test  "));

        assertEquals("An OTP has been sent to your email.", response.getMessage());
        verifyNoInteractions(authenticationManager);
        verify(otpRepository).deleteByEmail("Mika@Example.test");

        ArgumentCaptor<OtpCode> otpCaptor = ArgumentCaptor.forClass(OtpCode.class);
        verify(otpRepository).save(otpCaptor.capture());
        OtpCode savedOtp = otpCaptor.getValue();
        assertTrue(savedOtp.getCode().matches("\\d{6}"));
        assertEquals("Mika@Example.test", savedOtp.getEmail());
        assertTrue(savedOtp.getExpirationTime().isAfter(LocalDateTime.now().plusMinutes(4)));

        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mailCaptor.capture());
        assertEquals("Mika@Example.test", mailCaptor.getValue().getTo()[0]);
        assertTrue(mailCaptor.getValue().getText().contains(savedOtp.getCode()));
    }

    @Test
    void lockedAccountCannotRequestAnOtp() {
        User locked = User.builder()
                .id(8L)
                .username("locked")
                .email("locked@example.test")
                .passwordHash("hash")
                .isActive(false)
                .build();
        when(userRepository.findByEmailIgnoreCase("locked@example.test")).thenReturn(Optional.of(locked));

        assertThrows(AccessDeniedException.class,
                () -> service.generateOtpForEmail(new RequestOtpRequest("locked@example.test")));
        verify(otpRepository, never()).save(any(OtpCode.class));
        verifyNoInteractions(mailSender, authenticationManager);
    }
}
