package com.mangastudio.backend.repository;

import com.mangastudio.backend.entity.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PageRepository extends JpaRepository<Page, Long> {
    List<Page> findByChapterIdOrderByPageNumberAsc(Long chapterId);
}