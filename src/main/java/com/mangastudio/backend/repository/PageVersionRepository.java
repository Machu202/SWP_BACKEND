package com.mangastudio.backend.repository;

import com.mangastudio.backend.entity.PageVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PageVersionRepository extends JpaRepository<PageVersion, Long> {
    
    // Find all versions of a specific page, ordered by creation time descending (newest first)
    List<PageVersion> findByPageIdOrderByCreatedAtDesc(Long pageId);
    int countByPageId(Long pageId);
}