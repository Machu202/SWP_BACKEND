package com.mangastudio.backend.repository;

import com.mangastudio.backend.entity.ChapterScript;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ChapterScriptRepository extends JpaRepository<ChapterScript, Long> {
    Optional<ChapterScript> findByChapterId(Long chapterId);
}