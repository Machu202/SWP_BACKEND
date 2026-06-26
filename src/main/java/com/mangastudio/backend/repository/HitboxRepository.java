package com.mangastudio.backend.repository;

import com.mangastudio.backend.entity.Hitbox;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HitboxRepository extends JpaRepository<Hitbox, Long> {
    // Lấy danh sách các hitbox dựa trên ID của trang truyện
    List<Hitbox> findByPageId(Long pageId);
}