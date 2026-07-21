package com.mangastudio.backend.service;

import com.mangastudio.backend.entity.DeadlineEvent;
import java.time.LocalDateTime;
import java.util.List;

public interface DeadlineEventService {
    DeadlineEvent createDeadline(Long seriesId, Long mangakaId, String eventName, LocalDateTime deadlineDate, String warningLevel);
    List<DeadlineEvent> getDeadlinesBySeries(Long seriesId);
    void deleteDeadline(Long eventId, Long mangakaId);
    
    // Spring Boot invokes this scheduled job automatically.
    void scanAndWarnDeadlines(); 
}
