package com.mangastudio.backend.repository;

import com.mangastudio.backend.entity.MangaSeries;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MangaSeriesRepository extends JpaRepository<MangaSeries, Long> {
    // Lấy danh sách truyện của một tác giả cụ thể
    List<MangaSeries> findByMangaka_UserId(Long mangakaId);
}