package com.mangastudio.backend.repository;

import com.mangastudio.backend.entity.DeadlineEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface DeadlineEventRepository extends JpaRepository<DeadlineEvent, Long> {
    // Returns deadlines for a project.
    List<DeadlineEvent> findByMangaSeriesIdOrderByDeadlineDateAsc(Long seriesId);

    void deleteByMangaSeriesId(Long seriesId);

    void deleteByMangaSeriesIdAndEventName(Long seriesId, String eventName);
    
    // Scheduled-job query: finds overdue deadlines that have not been reported.
    List<DeadlineEvent> findByWarningLevelNotAndDeadlineDateBefore(String warningLevel, LocalDateTime currentTime);
}
