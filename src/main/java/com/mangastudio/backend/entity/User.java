package com.mangastudio.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    // Liên kết khóa ngoại N-1 với bảng Role
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    // ... existing fields (userId, username, password, email, role)

    @Column(name = "otp_code", length = 6)
    private String otpCode;

    @Column(name = "otp_expiration")
    private java.time.LocalDateTime otpExpiration;
}