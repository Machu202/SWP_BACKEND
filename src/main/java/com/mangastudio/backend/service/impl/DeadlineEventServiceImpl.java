package com.mangastudio.backend.service.impl;

import com.mangastudio.backend.entity.DeadlineEvent;
import com.mangastudio.backend.entity.MangaSeries;
import com.mangastudio.backend.repository.DeadlineEventRepository;
import com.mangastudio.backend.repository.MangaSeriesRepository;
import com.mangastudio.backend.service.DeadlineEventService;
import com.mangastudio.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeadlineEventServiceImpl implements DeadlineEventService {

    private final DeadlineEventRepository deadlineEventRepository;
    private final MangaSeriesRepository mangaSeriesRepository;
    private final NotificationService notificationService;

    // Trong file DeadlineEventServiceImpl.java, CHỈ GIỮ LẠI HÀM NÀY:

    @Override
    @Transactional
    public DeadlineEvent createDeadline(Long seriesId, Long mangakaId, String eventName, LocalDateTime deadlineDate, String warningLevel) {
        MangaSeries series = mangaSeriesRepository.findById(seriesId)
            .orElseThrow(() -> new RuntimeException("Error: Manga Series not found"));
        if (!series.getMangaka().getId().equals(mangakaId)) {
        throw new RuntimeException("Error: Only the Mangaka can set deadlines for this series.");
    }

    DeadlineEvent event = DeadlineEvent.builder()
            .mangaSeries(series)
            .eventName(eventName)
            .deadlineDate(deadlineDate)
            .warningLevel(warningLevel != null && !warningLevel.isBlank() ? warningLevel.toUpperCase() : "NORMAL")
            .build();
    return deadlineEventRepository.save(event);
    }

    @Override
    public List<DeadlineEvent> getDeadlinesBySeries(Long seriesId) {
        return deadlineEventRepository.findByMangaSeriesIdOrderByDeadlineDateAsc(seriesId);
    }

    @Override
    @Transactional
    public void deleteDeadline(Long eventId, Long mangakaId) {
        DeadlineEvent event = deadlineEventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Error: Deadline not found"));

        if (!event.getMangaSeries().getMangaka().getId().equals(mangakaId)) {
            throw new RuntimeException("Error: You do not have permission to delete this deadline.");
        }
        deadlineEventRepository.delete(event);
    }

    // ==========================================
    // [FE-23] CRON JOB: Chạy tự động mỗi 1 phút
    // ==========================================
    @Override
    @Scheduled(fixedRate = 60000) // 60,000 milliseconds = 1 minute
    @Transactional
    public void scanAndWarnDeadlines() {
        // 1. Tìm tất cả các sự kiện đã qua thời gian hiện tại nhưng chưa bị dán nhãn "TRIGGERED"
        List<DeadlineEvent> overdueEvents = deadlineEventRepository
                .findByWarningLevelNotAndDeadlineDateBefore("TRIGGERED", LocalDateTime.now());

        if (overdueEvents.isEmpty()) return;

        for (DeadlineEvent event : overdueEvents) {
            Long mangakaId = event.getMangaSeries().getMangaka().getId();
            String seriesTitle = event.getMangaSeries().getTitle();

            // 2. Soạn tin nhắn cảnh báo
            String warningMsg = "🚨 [URGENT DEADLINE] The project '" + seriesTitle + "' has an overdue event: " + event.getEventName() + "!";

            // 3. Bắn Notification (Hàm này trong NotificationService đã gắn sẵn WebSocket)
            notificationService.createNotification(mangakaId, warningMsg);

            // 4. Khóa sự kiện lại thành TRIGGERED để phút sau nó không bắn spam báo động nữa
            event.setWarningLevel("TRIGGERED");
            deadlineEventRepository.save(event);
            
            System.out.println(">>> [CRON JOB] Fired deadline warning for event: " + event.getEventName());
        }
    }
}