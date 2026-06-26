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

    // Frontend sẽ gọi API này ngầm (không cần token) mỗi khi người đọc cuộn tới một chapter
    @PostMapping("/chapters/{chapterId}/view")
    public ResponseEntity<String> recordView(@PathVariable Long chapterId) {
        telemetryBufferService.recordChapterView(chapterId);
        return ResponseEntity.ok("View recorded in buffer.");
    }

    // Mangaka hoặc Admin xem thống kê tổng của một bộ truyện
    @GetMapping("/series/{seriesId}")
    public ResponseEntity<TelemetryAnalytics> getAnalytics(@PathVariable Long seriesId) {
        TelemetryAnalytics analytics = telemetryAnalyticsRepository.findByMangaSeriesId(seriesId)
                .orElseThrow(() -> new RuntimeException("Error: No telemetry data found for this series."));
        return ResponseEntity.ok(analytics);
    }
}