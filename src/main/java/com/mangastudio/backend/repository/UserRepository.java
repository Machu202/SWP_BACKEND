package com.mangastudio.backend.repository;

import com.mangastudio.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    
    // Spring Data JPA will automatically generate the SQL query for this method
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);

    // Useful for validation during registration (FE-01)
    Boolean existsByUsername(String username);
    
    Boolean existsByEmail(String email);
}