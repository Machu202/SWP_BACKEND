package com.mangastudio.backend.repository;

import com.mangastudio.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
    
    // Bổ sung 2 hàm kiểm tra tồn tại (Trả về true/false cực nhanh)
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
}