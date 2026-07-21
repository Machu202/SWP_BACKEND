package com.mangastudio.backend.controller;

import com.mangastudio.backend.entity.TelemetryAnalytics;
import com.mangastudio.backend.repository.TelemetryAnalyticsRepository;
import com.mangastudio.backend.service.TelemetryBufferService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/telemetry")
@RequiredArgsConstructor
public class TelemetryController {

    private final TelemetryBufferService telemetryBufferService;
    private final TelemetryAnalyticsRepository telemetryAnalyticsRepository;

    // The frontend calls this public endpoint when a reader reaches a chapter.
    @PostMapping("/chapters/{chapterId}/view")
    public ResponseEntity<String> recordView(@PathVariable Long chapterId) {
        telemetryBufferService.recordChapterView(chapterId);
        return ResponseEntity.ok("View recorded in buffer.");
    }

    // Allows the Mangaka or Admin to view aggregate series statistics.
    @GetMapping("/series/{seriesId}")
    public ResponseEntity<TelemetryAnalytics> getAnalytics(@PathVariable Long seriesId) {
        TelemetryAnalytics analytics = telemetryAnalyticsRepository.findByMangaSeriesId(seriesId)
                .orElseThrow(() -> new RuntimeException("Error: No telemetry data found for this series."));
        return ResponseEntity.ok(analytics);
    }
}
