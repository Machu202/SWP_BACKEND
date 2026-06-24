package com.mangastudio.backend.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class ErrorResponse {
    
    private int status;           // Mã HTTP (VD: 400, 404, 500)
    private String message;       // Thông điệp lỗi chi tiết
    private LocalDateTime timestamp; // Thời điểm xảy ra lỗi
}