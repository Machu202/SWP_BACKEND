package com.mangastudio.backend.service.impl;

import com.mangastudio.backend.dto.request.PublishingScheduleRequest;
import com.mangastudio.backend.entity.MangaSeries;
import com.mangastudio.backend.entity.PublishingSchedule;
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

    @Override
    @Transactional
    public PublishingSchedule createSchedule(Long currentUserId, PublishingScheduleRequest request) {
        MangaSeries series = mangaSeriesRepository.findById(request.getSeriesId())
                .orElseThrow(() -> new RuntimeException("Error: Manga Series not found"));

        // Khóa bảo mật: Chỉ Mangaka của bộ truyện mới được tạo lịch
        if (!series.getMangaka().getId().equals(currentUserId)) {
            throw new RuntimeException("Error: You do not have permission to schedule this series");
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

        scheduleRepository.delete(schedule);
    }
}