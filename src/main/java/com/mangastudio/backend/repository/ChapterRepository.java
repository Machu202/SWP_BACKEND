package com.mangastudio.backend.repository;

import com.mangastudio.backend.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChapterRepository extends JpaRepository<Chapter, Long> {
    
    // Lấy toàn bộ chapter của một bộ truyện, sắp xếp theo số chapter tăng dần
    List<Chapter> findByMangaSeriesIdOrderByChapterNumberAsc(Long seriesId);
}