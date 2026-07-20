package com.mangastudio.backend.repository;

import com.mangastudio.backend.entity.TelemetryAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TelemetryAnalyticsRepository extends JpaRepository<TelemetryAnalytics, Long> {
    // Tìm bản ghi thống kê của một bộ truyện
    Optional<TelemetryAnalytics> findByMangaSeriesId(Long seriesId);

    void deleteByMangaSeriesId(Long seriesId);
}
