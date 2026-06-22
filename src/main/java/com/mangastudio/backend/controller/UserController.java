package com.mangastudio.backend.controller;

import com.mangastudio.backend.dto.AdminUserCreateRequest;
import com.mangastudio.backend.entity.Role;
import com.mangastudio.backend.entity.User;
import com.mangastudio.backend.repository.RoleRepository;
import com.mangastudio.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    // ==========================================
    // 1. GET ALL USERS (Admin Dashboard)
    // ==========================================
    @GetMapping
    public ResponseEntity<?> getAllUsers() {
        List<User> users = userRepository.findAll();
        
        // Note: For security in production, you should map this to a UserResponse DTO to hide passwords.
        // For tomorrow's demo, returning the raw entity is acceptable for speed.
        return ResponseEntity.ok(users);
    }

    // ==========================================
    // 2. ADMIN CREATE USER WITH SPECIFIC ROLE
    // ==========================================
    @PostMapping("/admin/create")
    public ResponseEntity<?> createUserByAdmin(@RequestBody AdminUserCreateRequest request) {
        
        // Step 1: Validate Duplicates
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest().body("Username already exists!");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body("Email is already in use!");
        }

        // Step 2: Validate Role Existence
        Role assignedRole = roleRepository.findByRoleName(request.getRoleName());
        if (assignedRole == null) {
            return ResponseEntity.badRequest().body("The specified role does not exist in the system!");
        }

        // Step 3: Create User
        User newUser = new User();
        newUser.setUsername(request.getUsername());
        newUser.setPassword(request.getPassword()); // Should be hashed in production
        newUser.setEmail(request.getEmail());
        newUser.setRole(assignedRole);

        userRepository.save(newUser);

        return ResponseEntity.status(HttpStatus.CREATED).body("User created successfully by Admin.");
    }
    
    // ==========================================
    // 3. DELETE USER (Admin Only)
    // ==========================================
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User ID not found!");
        }
        
        userRepository.deleteById(id);
        return ResponseEntity.ok("User deleted successfully.");
    }
}