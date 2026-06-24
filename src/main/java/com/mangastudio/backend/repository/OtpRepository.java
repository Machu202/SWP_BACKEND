package com.mangastudio.backend.repository;

import com.mangastudio.backend.entity.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpRepository extends JpaRepository<OtpCode, Long> {
    
    Optional<OtpCode> findByEmailAndCode(String email, String code);
    
    // Used to clear old OTPs before generating a new one
    void deleteByEmail(String email);
}