package com.mangastudio.backend.component;

import com.mangastudio.backend.entity.PublishingSchedule;
import com.mangastudio.backend.repository.PublishingScheduleRepository;
import com.mangastudio.backend.service.ChapterService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ChapterPublicationScheduler {

    private final PublishingScheduleRepository publishingScheduleRepository;
    private final ChapterService chapterService;

    @Scheduled(fixedDelay = 5000)
    public void publishDueChapters() {
        for (PublishingSchedule schedule : publishingScheduleRepository.findDueChapterLaunches(
                "CHAPTER_LAUNCH", LocalDateTime.now())) {
            try {
                chapterService.publishScheduledChapter(schedule.getChapter().getId());
            } catch (RuntimeException exception) {
                System.err.println(">>> [CHAPTER PUBLICATION] Could not publish schedule "
                        + schedule.getId() + ": " + exception.getMessage());
            }
        }
    }
}
