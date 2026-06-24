package com.mangastudio.backend.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserProfileResponse {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String roleName;
    private String profileData;
    private Boolean isActive; // [BỔ SUNG FE-03]
}