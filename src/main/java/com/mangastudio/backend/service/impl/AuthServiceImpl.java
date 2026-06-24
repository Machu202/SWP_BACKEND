package com.mangastudio.backend.service.impl;

import com.mangastudio.backend.dto.request.LoginRequest;
import com.mangastudio.backend.dto.request.RegisterRequest;
import com.mangastudio.backend.dto.response.JwtResponse;
import com.mangastudio.backend.dto.response.MessageResponse;
import com.mangastudio.backend.entity.Role;
import com.mangastudio.backend.entity.User;
import com.mangastudio.backend.repository.RoleRepository;
import com.mangastudio.backend.repository.UserRepository;
import com.mangastudio.backend.security.JwtUtils;
import com.mangastudio.backend.security.UserDetailsImpl;
import com.mangastudio.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;

    @Override
    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        // 1. Verify username and password using Spring Security
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        // 2. Set authentication in context
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 3. Generate JWT Token
        String jwt = jwtUtils.generateJwtToken(authentication);

        // 4. Extract user details
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String role = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_USER");

        return new JwtResponse(jwt, userDetails.getId(), userDetails.getUsername(), userDetails.getEmail(), role);
    }

    @Override
    @Transactional
    public MessageResponse registerUser(RegisterRequest request) {
        // 1. Check if username or email already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Error: Username is already taken!");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Error: Email is already in use!");
        }

        // 2. Find the requested role in DB
        Role userRole = roleRepository.findByRoleName(request.getRole())
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));

        // 3. Create new user account with hashed password
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(encoder.encode(request.getPassword()))
                .role(userRole)
                .build();

        userRepository.save(user);

        return new MessageResponse("User registered successfully!");
    }
}