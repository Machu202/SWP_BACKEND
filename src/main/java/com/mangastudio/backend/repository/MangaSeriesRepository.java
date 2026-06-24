package com.mangastudio.backend.repository;

import com.mangastudio.backend.entity.MangaSeries;
import org.springframework.data.jpa.repository.JpaRepository;
public interface MangaSeriesRepository extends JpaRepository<MangaSeries, Long> {
}