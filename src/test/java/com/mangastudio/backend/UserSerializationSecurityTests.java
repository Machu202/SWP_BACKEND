package com.mangastudio.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mangastudio.backend.entity.Hitbox;
import com.mangastudio.backend.entity.Role;
import com.mangastudio.backend.entity.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserSerializationSecurityTests {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void directUserSerializationNeverExposesPasswordHash() throws Exception {
        User user = User.builder()
                .id(3L)
                .username("assistant")
                .email("assistant@example.com")
                .passwordHash("bcrypt-secret-value")
                .role(Role.builder().id(2L).roleName("Assistant").build())
                .isActive(true)
                .build();

        String json = objectMapper.writeValueAsString(user);

        assertFalse(json.contains("passwordHash"));
        assertFalse(json.contains("bcrypt-secret-value"));
        assertTrue(json.contains("assistant@example.com"));
    }

    @Test
    void nestedUserSerializationNeverExposesPasswordHash() throws Exception {
        User creator = User.builder()
                .id(2L)
                .username("mangaka")
                .passwordHash("nested-secret-value")
                .role(Role.builder().id(1L).roleName("Mangaka").build())
                .isActive(true)
                .build();

        Hitbox hitbox = Hitbox.builder()
                .id(10L)
                .createdBy(creator)
                .xCoord(0.1)
                .yCoord(0.2)
                .width(0.3)
                .height(0.4)
                .build();

        String json = objectMapper.writeValueAsString(hitbox);

        assertFalse(json.contains("passwordHash"));
        assertFalse(json.contains("nested-secret-value"));
        assertTrue(json.contains("createdBy"));
    }
}
