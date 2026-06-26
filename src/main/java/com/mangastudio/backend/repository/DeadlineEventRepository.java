package com.mangastudio.backend.repository;

import com.mangastudio.backend.entity.DeadlineEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface DeadlineEventRepository extends JpaRepository<DeadlineEvent, Long> {
    // Lấy danh sách deadline của một dự án
    List<DeadlineEvent> findByMangaSeriesIdOrderByDeadlineDateAsc(Long seriesId);
    
    // Câu query thần thánh của Cron Job: Tìm các deadline ĐÃ TRỄ HẠN nhưng CHƯA ĐƯỢC CẢNH BÁO
    List<DeadlineEvent> findByWarningLevelNotAndDeadlineDateBefore(String warningLevel, LocalDateTime currentTime);
}