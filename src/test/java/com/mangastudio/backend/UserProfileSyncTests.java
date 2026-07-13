package com.mangastudio.backend;

import com.mangastudio.backend.dto.request.UserProfileUpdateRequest;
import com.mangastudio.backend.entity.Role;
import com.mangastudio.backend.entity.User;
import com.mangastudio.backend.repository.RoleRepository;
import com.mangastudio.backend.repository.UserRepository;
import com.mangastudio.backend.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class UserProfileSyncTests {

    private UserRepository userRepository;
    private UserServiceImpl service;
    private User user;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        service = new UserServiceImpl(userRepository, roleRepository);
        user = User.builder()
                .id(4L)
                .username("HuyTantou")
                .email("HuyTantou@gmail.com")
                .phoneNumber("0987000000")
                .fullName("Huy Tantou")
                .profileData("{\"bio\":\"editor\"}")
                .createdAt(LocalDateTime.of(2026, 7, 1, 10, 30))
                .isActive(true)
                .role(Role.builder().id(3L).roleName("Tantou Editor").build())
                .passwordHash("not-serialized")
                .build();
    }

    @Test
    void profileResponseContainsSupabasePhoneAndCreatedAt() {
        when(userRepository.findByUsername("HuyTantou")).thenReturn(Optional.of(user));

        var response = service.getUserProfile("HuyTantou");

        assertEquals("0987000000", response.getPhoneNumber());
        assertEquals("Huy Tantou", response.getFullName());
        assertEquals("HuyTantou@gmail.com", response.getEmail());
        assertNotNull(response.getCreatedAt());
    }

    @Test
    void profileUpdatePersistsPhoneFullNameAndProfileData() {
        when(userRepository.findByUsername("HuyTantou")).thenReturn(Optional.of(user));
        when(userRepository.existsByPhoneNumber("0909000000")).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);

        UserProfileUpdateRequest request = new UserProfileUpdateRequest();
        request.setFullName(" Updated Name ");
        request.setPhoneNumber(" 0909000000 ");
        request.setProfileData(" updated profile ");

        var response = service.updateUserProfile("HuyTantou", request);

        assertEquals("Updated Name", response.getFullName());
        assertEquals("0909000000", response.getPhoneNumber());
        assertEquals("updated profile", response.getProfileData());
        verify(userRepository).save(user);
    }
}
