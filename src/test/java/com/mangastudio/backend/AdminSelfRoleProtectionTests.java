package com.mangastudio.backend;

import com.mangastudio.backend.entity.Role;
import com.mangastudio.backend.entity.User;
import com.mangastudio.backend.repository.RoleRepository;
import com.mangastudio.backend.repository.UserRepository;
import com.mangastudio.backend.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AdminSelfRoleProtectionTests {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        service = new UserServiceImpl(userRepository, roleRepository);
    }

    @Test
    void rejectsChangingTheAuthenticatedAdminsOwnRole() {
        AccessDeniedException error = assertThrows(AccessDeniedException.class, () ->
                service.assignRole(5L, "Mangaka", 5L));

        assertEquals("Admin cannot change their own role.", error.getMessage());
        verifyNoInteractions(userRepository, roleRepository);
    }

    @Test
    void stillAllowsAdminToChangeAnotherUsersRole() {
        User user = User.builder()
                .id(8L)
                .username("member")
                .role(Role.builder().roleName("Mangaka").build())
                .build();
        Role assistant = Role.builder().roleName("Assistant").build();
        when(userRepository.findById(8L)).thenReturn(Optional.of(user));
        when(roleRepository.findByRoleName("Assistant")).thenReturn(Optional.of(assistant));
        when(userRepository.save(user)).thenReturn(user);

        assertEquals("Assistant", service.assignRole(8L, "Assistant", 5L).getRoleName());
    }
}
