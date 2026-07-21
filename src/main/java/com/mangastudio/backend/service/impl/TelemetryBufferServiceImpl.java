package com.mangastudio.backend.service.impl;

import com.mangastudio.backend.entity.Chapter;
import com.mangastudio.backend.entity.MangaSeries;
import com.mangastudio.backend.entity.TelemetryAnalytics;
import com.mangastudio.backend.repository.ChapterRepository;
import com.mangastudio.backend.repository.MangaSeriesRepository;
import com.mangastudio.backend.repository.TelemetryAnalyticsRepository;
import com.mangastudio.backend.service.TelemetryBufferService;
import com.mangastudio.backend.component.ConfigurableSchedulerGate;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class TelemetryBufferServiceImpl implements TelemetryBufferService {

    private final ChapterRepository chapterRepository;
    private final MangaSeriesRepository mangaSeriesRepository;
    private final TelemetryAnalyticsRepository telemetryRepository;
    private final ConfigurableSchedulerGate schedulerGate;

    // L1 memory buffer: temporarily stores views by chapter ID.
    private final Map<Long, AtomicLong> viewBuffer = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public void initializeSeries(Long seriesId) {
        if (telemetryRepository.findByMangaSeriesId(seriesId).isPresent()) return;
        MangaSeries series = mangaSeriesRepository.findById(seriesId)
                .orElseThrow(() -> new RuntimeException("Error: Manga Series not found"));
        telemetryRepository.save(TelemetryAnalytics.builder()
                .mangaSeries(series)
                .recordedBy(series.getMangaka())
                .readerVotes(0)
                .views(0)
                .calculatedAt(LocalDateTime.now())
                .build());
    }

    @Override
    public void recordChapterView(Long chapterId) {
        // Increment the in-memory view count without writing to the database.
        viewBuffer.computeIfAbsent(chapterId, k -> new AtomicLong(0)).incrementAndGet();
    }

    // The one-second heartbeat is gated by TELEMETRY_FLUSH_SECONDS.
    @Override
    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void flushBufferToDatabase() {
        if (!schedulerGate.shouldRun("telemetry-flush", "TELEMETRY_FLUSH_SECONDS", 180)) return;
        if (viewBuffer.isEmpty()) return;

        viewBuffer.forEach((chapterId, count) -> {
            // Read the current view count and reset it to zero in memory.
            long viewsToFlush = count.getAndSet(0);
            
            if (viewsToFlush > 0) {
                try {
                    // 1. Resolve the manga series through the chapter.
                    Chapter chapter = chapterRepository.findById(chapterId).orElse(null);
                    if (chapter == null) return;
                    
                    MangaSeries series = chapter.getMangaSeries();

                    // 2. Update the existing telemetry row, or create one if needed.
                    TelemetryAnalytics analytics = telemetryRepository.findByMangaSeriesId(series.getId())
                            .orElse(TelemetryAnalytics.builder()
                                    .mangaSeries(series)
                                    .recordedBy(series.getMangaka()) // The Mangaka owns this telemetry record.
                                    .readerVotes(0)
                                    .views(0)
                                    .build());

                    // 3. Accumulate the views.
                    analytics.setViews(analytics.getViews() + (int) viewsToFlush);
                    analytics.setCalculatedAt(LocalDateTime.now());

                    telemetryRepository.save(analytics);
                    
                    System.out.println(">>> [TELEMETRY FLUSH] Saved " + viewsToFlush + " views for Series: " + series.getTitle());
                } catch (Exception e) {
                    System.err.println(">>> [TELEMETRY ERROR] Failed to flush views for chapter " + chapterId + ": " + e.getMessage());
                    // Restore views to memory so a later flush can retry after a database failure.
                    viewBuffer.get(chapterId).addAndGet(viewsToFlush);
                }
            }
        });
    }
}
