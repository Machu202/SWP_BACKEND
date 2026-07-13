package com.mangastudio.backend.repository;

import com.mangastudio.backend.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChapterRepository extends JpaRepository<Chapter, Long> {
    List<Chapter> findByMangaSeriesIdOrderByChapterNumberAsc(Long seriesId);

    // Only chapters belonging to series assigned to the authenticated Tantou.
    List<Chapter> findByMangaSeries_Tantou_IdOrderByChapterNumberAsc(Long tantouId);
}
