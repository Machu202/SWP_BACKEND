package com.mangastudio.backend.service;

import com.mangastudio.backend.entity.DeadlineEvent;
import java.time.LocalDateTime;
import java.util.List;

public interface DeadlineEventService {
    DeadlineEvent createDeadline(Long seriesId, Long mangakaId, String eventName, LocalDateTime deadlineDate, String warningLevel);
    List<DeadlineEvent> getDeadlinesBySeries(Long seriesId);
    void deleteDeadline(Long eventId, Long mangakaId);
    
    // Hàm này sẽ được Spring Boot tự động gọi ngầm (Cron Job)
    void scanAndWarnDeadlines(); 
}