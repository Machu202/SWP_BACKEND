package com.mangastudio.backend.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class ErrorResponse {
    
    private int status;           // HTTP status code (for example, 400, 404, or 500)
    private String message;       // Detailed error message
    private LocalDateTime timestamp; // Time when the error occurred
}
