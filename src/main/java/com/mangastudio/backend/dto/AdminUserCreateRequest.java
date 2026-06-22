package com.mangastudio.backend.dto;

import lombok.Data;

@Data
public class AdminUserCreateRequest {
    private String username;
    private String password;
    private String email;
    private String roleName; // E.g., "TANTOU_EDITOR", "ASSISTANT"
}