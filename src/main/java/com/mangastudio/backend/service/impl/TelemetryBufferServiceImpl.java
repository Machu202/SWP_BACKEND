package com.mangastudio.backend.service.impl;

import com.mangastudio.backend.entity.Chapter;
import com.mangastudio.backend.entity.MangaSeries;
import com.mangastudio.backend.entity.TelemetryAnalytics;
import com.mangastudio.backend.repository.ChapterRepository;
import com.mangastudio.backend.repository.MangaSeriesRepository;
import com.mangastudio.backend.repository.TelemetryAnalyticsRepository;
import com.mangastudio.backend.service.TelemetryBufferService;
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

    // L1 RAM Buffer: Lưu trữ tạm thời lượt xem theo ID của Chapter
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
        // Tăng lượt xem trong RAM lên 1, cực kỳ nhanh, không đụng tới DB
        viewBuffer.computeIfAbsent(chapterId, k -> new AtomicLong(0)).incrementAndGet();
    }

    // Cron Job: Cứ 3 phút (180,000 ms) xả đệm một lần
    @Override
    @Scheduled(fixedRate = 180000)
    @Transactional
    public void flushBufferToDatabase() {
        if (viewBuffer.isEmpty()) return;

        viewBuffer.forEach((chapterId, count) -> {
            // Lấy ra số lượt xem hiện tại và reset về 0 trong RAM
            long viewsToFlush = count.getAndSet(0);
            
            if (viewsToFlush > 0) {
                try {
                    // 1. Lấy thông tin Chapter để dò ra Manga Series
                    Chapter chapter = chapterRepository.findById(chapterId).orElse(null);
                    if (chapter == null) return;
                    
                    MangaSeries series = chapter.getMangaSeries();

                    // 2. Lấy bản ghi Telemetry cũ của truyện, nếu chưa có thì tạo mới
                    TelemetryAnalytics analytics = telemetryRepository.findByMangaSeriesId(series.getId())
                            .orElse(TelemetryAnalytics.builder()
                                    .mangaSeries(series)
                                    .recordedBy(series.getMangaka()) // Lấy Mangaka làm người sở hữu record
                                    .readerVotes(0)
                                    .views(0)
                                    .build());

                    // 3. Cộng dồn lượt xem
                    analytics.setViews(analytics.getViews() + (int) viewsToFlush);
                    analytics.setCalculatedAt(LocalDateTime.now());

                    telemetryRepository.save(analytics);
                    
                    System.out.println(">>> [TELEMETRY FLUSH] Saved " + viewsToFlush + " views for Series: " + series.getTitle());
                } catch (Exception e) {
                    System.err.println(">>> [TELEMETRY ERROR] Failed to flush views for chapter " + chapterId + ": " + e.getMessage());
                    // Bồi hoàn lại lượt xem vào RAM để lần sau flush tiếp nếu DB bị lỗi mạng
                    viewBuffer.get(chapterId).addAndGet(viewsToFlush);
                }
            }
        });
    }
}
