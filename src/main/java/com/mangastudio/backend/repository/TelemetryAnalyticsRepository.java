package com.mangastudio.backend.repository;

import com.mangastudio.backend.entity.TelemetryAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TelemetryAnalyticsRepository extends JpaRepository<TelemetryAnalytics, Long> {
    // Finds the telemetry record for a manga series.
    Optional<TelemetryAnalytics> findByMangaSeriesId(Long seriesId);

    void deleteByMangaSeriesId(Long seriesId);
}
