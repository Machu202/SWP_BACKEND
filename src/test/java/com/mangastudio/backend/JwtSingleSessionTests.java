package com.mangastudio.backend;

import com.mangastudio.backend.entity.Role;
import com.mangastudio.backend.entity.User;
import com.mangastudio.backend.security.JwtUtils;
import com.mangastudio.backend.security.UserDetailsImpl;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtSingleSessionTests {
    @Test
    void jwtCarriesTheDatabaseBackedSessionIdentifier() {
        User user = User.builder().id(1L).username("single-session").email("test@example.com")
                .passwordHash("x").isActive(true)
                .role(Role.builder().id(1L).roleName("Assistant").build()).build();
        var principal = UserDetailsImpl.build(user);
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        JwtUtils jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", Base64.getEncoder().encodeToString(
                "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8)));
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 3_600_000);

        String token = jwtUtils.generateJwtToken(authentication, "newest-session");
        assertTrue(jwtUtils.validateJwtToken(token));
        assertEquals("newest-session", jwtUtils.getSessionIdFromJwtToken(token));
    }
}
