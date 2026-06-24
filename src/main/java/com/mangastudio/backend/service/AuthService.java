package com.mangastudio.backend.service;

import com.mangastudio.backend.dto.request.LoginRequest;
import com.mangastudio.backend.dto.request.RegisterRequest;
import com.mangastudio.backend.dto.response.JwtResponse;
import com.mangastudio.backend.dto.response.MessageResponse;

public interface AuthService {
    JwtResponse authenticateUser(LoginRequest loginRequest);
    MessageResponse registerUser(RegisterRequest registerRequest);
}