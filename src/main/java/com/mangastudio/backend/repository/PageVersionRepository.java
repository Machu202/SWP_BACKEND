package com.mangastudio.backend.repository;

import com.mangastudio.backend.entity.PageVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PageVersionRepository extends JpaRepository<PageVersion, Long> {
    List<PageVersion> findByPageIdOrderByCreatedAtDesc(Long pageId);
}