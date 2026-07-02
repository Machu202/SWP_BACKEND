package com.mangastudio.backend.repository;

import com.mangastudio.backend.entity.PageVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PageVersionRepository extends JpaRepository<PageVersion, Long> {

    List<PageVersion> findByPageIdOrderByCreatedAtDesc(Long pageId);

    int countByPageId(Long pageId);

    Optional<PageVersion> findTopByPageIdOrderByVersionNumberDesc(Long pageId);
}
