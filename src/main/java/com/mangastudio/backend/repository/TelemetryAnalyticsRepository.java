package com.mangastudio.backend.repository;

import com.mangastudio.backend.entity.TelemetryAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TelemetryAnalyticsRepository extends JpaRepository<TelemetryAnalytics, Long> {
}