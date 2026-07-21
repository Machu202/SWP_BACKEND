package com.mangastudio.backend.repository;

import com.mangastudio.backend.entity.HitboxComment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HitboxCommentRepository extends JpaRepository<HitboxComment, Long> {
    // Returns every hitbox comment, sorted from oldest to newest.
    List<HitboxComment> findByHitboxIdOrderByCreatedAtAsc(Long hitboxId);
    void deleteByHitboxId(Long hitboxId);
}
