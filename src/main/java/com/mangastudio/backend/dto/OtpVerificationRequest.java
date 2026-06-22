package com.mangastudio.backend.dto;

import lombok.Data;

@Data
public class OtpVerificationRequest {
    private String username;
    private String otpCode;
}