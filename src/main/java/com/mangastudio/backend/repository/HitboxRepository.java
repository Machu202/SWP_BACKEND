package com.mangastudio.backend.repository;

import com.mangastudio.backend.entity.Hitbox;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HitboxRepository extends JpaRepository<Hitbox, Long> {
    List<Hitbox> findByPageId(Long pageId);
    List<Hitbox> findByPageIdAndPageVersionIsNull(Long pageId);
    List<Hitbox> findByPageVersionId(Long pageVersionId);
}
