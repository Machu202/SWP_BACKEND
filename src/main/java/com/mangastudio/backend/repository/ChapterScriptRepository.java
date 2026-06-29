package com.mangastudio.backend.repository;

import java.util.List;
import com.mangastudio.backend.entity.ChapterScript;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ChapterScriptRepository extends JpaRepository<ChapterScript, Long> {
    Optional<ChapterScript> findByChapterId(Long chapterId);
    List<ChapterScript> findByChapter_MangaSeries_IdOrderByChapter_ChapterNumberAsc(Long seriesId);
}