package com.mangastudio.backend.repository;

import com.mangastudio.backend.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChapterRepository extends JpaRepository<Chapter, Long> {
}