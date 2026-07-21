package com.mangastudio.backend.repository;

import com.mangastudio.backend.entity.Page;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PageRepository extends JpaRepository<Page, Long> {
    
    // Returns every page in a chapter, sorted by ascending page number.
    List<Page> findByChapterIdOrderByPageNumberAsc(Long chapterId);

    boolean existsByChapterIdAndPageNumber(Long chapterId, Integer pageNumber);

    long countByChapterId(Long chapterId);
}
