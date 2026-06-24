package com.mangastudio.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoogleLoginRequest {
    
    @NotBlank(message = "Google ID Token cannot be blank")
    private String token;
}