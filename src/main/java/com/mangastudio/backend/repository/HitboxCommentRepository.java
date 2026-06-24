package com.mangastudio.backend.repository;

import com.mangastudio.backend.entity.HitboxComment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HitboxCommentRepository extends JpaRepository<HitboxComment, Long> {
}