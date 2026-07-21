package com.mangastudio.backend.service;

import com.mangastudio.backend.dto.request.RequestOtpRequest;
import com.mangastudio.backend.dto.request.RegisterRequest;
import com.mangastudio.backend.dto.request.VerifyOtpRequest;
import com.mangastudio.backend.dto.response.JwtResponse;
import com.mangastudio.backend.dto.response.MessageResponse;

public interface AuthService {
    // Modified: Now returns a message asking to check email instead of JWT
    MessageResponse generateOtpForEmail(RequestOtpRequest requestOtpRequest);
    
    // New: Verifies OTP and finally returns the JWT Token
    JwtResponse verifyOtpAndLogin(VerifyOtpRequest verifyOtpRequest);
    
    MessageResponse registerUser(RegisterRequest registerRequest);
    
    // [FE-01] Handles Google sign-in.
    JwtResponse googleLogin(com.mangastudio.backend.dto.request.GoogleLoginRequest request);
}
