package com.mangastudio.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "Username cannot be blank")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    // ĐÃ XÓA @NotBlank và @Email ĐỂ CHO PHÉP NHẬP RỖNG
    private String email;

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 6, max = 40, message = "Password must be at least 6 characters")
    private String password;

    // BỔ SUNG BIẾN SỐ ĐIỆN THOẠI
    private String phoneNumber;

    @NotBlank(message = "Role must be specified")
    private String role;
}