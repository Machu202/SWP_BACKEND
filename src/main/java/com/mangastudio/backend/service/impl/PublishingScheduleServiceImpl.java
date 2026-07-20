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

        // Khóa bảo mật: Chỉ Mangaka của bộ truyện mới được tạo lịch
        if (!series.getMangaka().getId().equals(currentUserId)) {
            throw new RuntimeException("Error: You do not have permission to schedule this series");
        }
        if ("SERIES_LAUNCH".equalsIgnoreCase(request.getFrequency())) {
            throw new RuntimeException("Error: Use the approved-series Publish workflow to schedule a series launch.");
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
        // Kiểm tra xem truyện có tồn tại không
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
        if ("SERIES_LAUNCH".equalsIgnoreCase(schedule.getFrequency())
                || "SERIES_LAUNCH".equalsIgnoreCase(request.getFrequency())) {
            throw new RuntimeException("Error: Manage the series launch from the approved-series Publish workflow.");
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
        }
        scheduleRepository.delete(schedule);
    }
}
