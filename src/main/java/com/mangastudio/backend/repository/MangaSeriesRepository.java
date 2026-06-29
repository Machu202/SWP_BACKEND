package com.mangastudio.backend.repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.mangastudio.backend.entity.MangaSeries;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import com.mangastudio.backend.repository.MangaSeriesRepository;
import java.util.List;
public interface MangaSeriesRepository extends JpaRepository<MangaSeries, Long> {
    List<MangaSeries> findByMangakaId(Long mangakaId);
    Page<MangaSeries> findByStatus(String status, Pageable pageable);
}