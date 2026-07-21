package com.mangastudio.backend.repository;

import com.mangastudio.backend.entity.PublishingSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface PublishingScheduleRepository extends JpaRepository<PublishingSchedule, Long> {
    
    // Finds a series publication schedule, sorted by the nearest date first.
    List<PublishingSchedule> findByMangaSeriesIdOrderByPublishDateAsc(Long seriesId);

    @Query("SELECT schedule FROM PublishingSchedule schedule JOIN FETCH schedule.mangaSeries "
            + "WHERE UPPER(schedule.frequency) = UPPER(:frequency) "
            + "AND schedule.publishDate <= :now ORDER BY schedule.publishDate ASC, schedule.id ASC")
    List<PublishingSchedule> findDueSeriesLaunches(@Param("frequency") String frequency,
                                                   @Param("now") LocalDateTime now);

    @Query("SELECT schedule FROM PublishingSchedule schedule JOIN FETCH schedule.mangaSeries JOIN FETCH schedule.chapter "
            + "WHERE UPPER(schedule.frequency) = UPPER(:frequency) "
            + "AND schedule.publishDate <= :now ORDER BY schedule.publishDate ASC, schedule.id ASC")
    List<PublishingSchedule> findDueChapterLaunches(@Param("frequency") String frequency,
                                                    @Param("now") LocalDateTime now);

    void deleteByMangaSeriesIdAndFrequencyIgnoreCase(Long seriesId, String frequency);

    void deleteByChapterIdAndFrequencyIgnoreCase(Long chapterId, String frequency);

    void deleteByMangaSeriesId(Long seriesId);
}
