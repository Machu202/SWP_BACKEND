package com.mangastudio.backend.repository;

import com.mangastudio.backend.entity.PublishingSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PublishingScheduleRepository extends JpaRepository<PublishingSchedule, Long> {
    
    // Tìm lịch phát hành theo ID truyện, sắp xếp ngày tháng tăng dần (gần nhất lên trước)
    List<PublishingSchedule> findByMangaSeriesIdOrderByPublishDateAsc(Long seriesId);

    void deleteByMangaSeriesId(Long seriesId);
}
