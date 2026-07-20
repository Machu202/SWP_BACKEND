package com.mangastudio.backend.repository;

import com.mangastudio.backend.entity.Page;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PageRepository extends JpaRepository<Page, Long> {
    
    // Lấy toàn bộ trang của một chapter, sắp xếp theo thứ tự trang tăng dần
    List<Page> findByChapterIdOrderByPageNumberAsc(Long chapterId);

    boolean existsByChapterIdAndPageNumber(Long chapterId, Integer pageNumber);
}
