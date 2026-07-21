package com.mangastudio.backend.component;

import com.mangastudio.backend.entity.PublishingSchedule;
import com.mangastudio.backend.repository.PublishingScheduleRepository;
import com.mangastudio.backend.service.MangaSeriesService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class SeriesPublicationScheduler {

    private final PublishingScheduleRepository publishingScheduleRepository;
    private final MangaSeriesService mangaSeriesService;
    private final ConfigurableSchedulerGate schedulerGate;

    @Scheduled(fixedDelay = 1000)
    public void publishDueSeries() {
        if (!schedulerGate.shouldRun("series-publication", "PUBLICATION_SCAN_SECONDS", 5)) return;
        for (PublishingSchedule schedule : publishingScheduleRepository.findDueSeriesLaunches(
                "SERIES_LAUNCH", LocalDateTime.now())) {
            try {
                mangaSeriesService.publishScheduledSeries(schedule.getMangaSeries().getId());
            } catch (RuntimeException exception) {
                System.err.println(">>> [SERIES PUBLICATION] Could not publish schedule "
                        + schedule.getId() + ": " + exception.getMessage());
            }
        }
    }
}
