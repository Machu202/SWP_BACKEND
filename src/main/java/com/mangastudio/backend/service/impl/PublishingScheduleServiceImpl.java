package com.mangastudio.backend.service.impl;

import com.mangastudio.backend.dto.request.PublishingScheduleRequest;
import com.mangastudio.backend.entity.MangaSeries;
import com.mangastudio.backend.entity.PublishingSchedule;
import com.mangastudio.backend.repository.ChapterRepository;
import com.mangastudio.backend.repository.MangaSeriesRepository;
import com.mangastudio.backend.repository.PublishingScheduleRepository;
import com.mangastudio.backend.service.PublishingScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PublishingScheduleServiceImpl implements PublishingScheduleService {

    private final PublishingScheduleRepository scheduleRepository;
    private final MangaSeriesRepository mangaSeriesRepository;
    private final ChapterRepository chapterRepository;

    @Override
    @Transactional
    public PublishingSchedule createSchedule(Long currentUserId, PublishingScheduleRequest request) {
        MangaSeries series = mangaSeriesRepository.findById(request.getSeriesId())
                .orElseThrow(() -> new RuntimeException("Error: Manga Series not found"));

        // Security check: only the series Mangaka can create its schedule.
        if (!series.getMangaka().getId().equals(currentUserId)) {
            throw new RuntimeException("Error: You do not have permission to schedule this series");
        }
        if (isReservedLaunchFrequency(request.getFrequency())) {
            throw new RuntimeException("Error: Use the dedicated publication workflow to schedule a launch.");
        }

        PublishingSchedule newSchedule = PublishingSchedule.builder()
                .mangaSeries(series)
                .publishDate(request.getPublishDate())
                .frequency(request.getFrequency())
                .build();

        return scheduleRepository.save(newSchedule);
    }

    @Override
    public List<PublishingSchedule> getSchedulesBySeries(Long seriesId) {
        // Verify that the manga series exists.
        mangaSeriesRepository.findById(seriesId)
                .orElseThrow(() -> new RuntimeException("Error: Manga Series not found"));
                
        return scheduleRepository.findByMangaSeriesIdOrderByPublishDateAsc(seriesId);
    }

    @Override
    @Transactional
    public PublishingSchedule updateSchedule(Long scheduleId, Long currentUserId, PublishingScheduleRequest request) {
        PublishingSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Error: Schedule not found"));

        if (!schedule.getMangaSeries().getMangaka().getId().equals(currentUserId)) {
            throw new RuntimeException("Error: You do not have permission to modify this schedule");
        }
        if (isReservedLaunchFrequency(schedule.getFrequency())
                || isReservedLaunchFrequency(request.getFrequency())) {
            throw new RuntimeException("Error: Manage launches from the dedicated publication workflow.");
        }

        schedule.setPublishDate(request.getPublishDate());
        schedule.setFrequency(request.getFrequency());

        return scheduleRepository.save(schedule);
    }

    @Override
    @Transactional
    public void deleteSchedule(Long scheduleId, Long currentUserId) {
        PublishingSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Error: Schedule not found"));

        if (!schedule.getMangaSeries().getMangaka().getId().equals(currentUserId)) {
            throw new RuntimeException("Error: You do not have permission to delete this schedule");
        }

        if ("SERIES_LAUNCH".equalsIgnoreCase(schedule.getFrequency())
                && "APPROVED".equalsIgnoreCase(schedule.getMangaSeries().getStatus())) {
            chapterRepository.findByMangaSeriesIdOrderByChapterNumberAsc(schedule.getMangaSeries().getId())
                    .stream()
                    .filter(chapter -> "SCHEDULED".equalsIgnoreCase(chapter.getPublishStatus()))
                    .findFirst()
                    .ifPresent(chapter -> {
                        chapter.setPublishStatus("APPROVED");
                        chapterRepository.save(chapter);
                    });
        } else if ("CHAPTER_LAUNCH".equalsIgnoreCase(schedule.getFrequency())
                && schedule.getChapter() != null
                && "SCHEDULED".equalsIgnoreCase(schedule.getChapter().getPublishStatus())) {
            schedule.getChapter().setPublishStatus("APPROVED");
            chapterRepository.save(schedule.getChapter());
        }
        scheduleRepository.delete(schedule);
    }

    private boolean isReservedLaunchFrequency(String frequency) {
        return "SERIES_LAUNCH".equalsIgnoreCase(frequency)
                || "CHAPTER_LAUNCH".equalsIgnoreCase(frequency);
    }
}
