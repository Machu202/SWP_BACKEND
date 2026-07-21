package com.mangastudio.backend.service.impl;

import com.mangastudio.backend.entity.DeadlineEvent;
import com.mangastudio.backend.entity.MangaSeries;
import com.mangastudio.backend.repository.DeadlineEventRepository;
import com.mangastudio.backend.repository.MangaSeriesRepository;
import com.mangastudio.backend.service.DeadlineEventService;
import com.mangastudio.backend.service.NotificationService;
import com.mangastudio.backend.service.RuntimeSystemParameterService;
import com.mangastudio.backend.component.ConfigurableSchedulerGate;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DeadlineEventServiceImpl implements DeadlineEventService {

    private final DeadlineEventRepository deadlineEventRepository;
    private final MangaSeriesRepository mangaSeriesRepository;
    private final NotificationService notificationService;
    private final ConfigurableSchedulerGate schedulerGate;
    private final RuntimeSystemParameterService runtimeParameters;

    // Creates a deadline event.

    @Override
    @Transactional
    public DeadlineEvent createDeadline(Long seriesId, Long mangakaId, String eventName, LocalDateTime deadlineDate, String warningLevel) {
        MangaSeries series = mangaSeriesRepository.findById(seriesId)
            .orElseThrow(() -> new RuntimeException("Error: Manga Series not found"));
        if (!series.getMangaka().getId().equals(mangakaId)) {
        throw new RuntimeException("Error: Only the Mangaka can set deadlines for this series.");
    }

    DeadlineEvent event = DeadlineEvent.builder()
            .mangaSeries(series)
            .eventName(eventName)
            .deadlineDate(deadlineDate)
            .warningLevel(warningLevel != null && !warningLevel.isBlank() ? warningLevel.toUpperCase() : "NORMAL")
            .build();
    return deadlineEventRepository.save(event);
    }

    @Override
    public List<DeadlineEvent> getDeadlinesBySeries(Long seriesId) {
        return deadlineEventRepository.findByMangaSeriesIdOrderByDeadlineDateAsc(seriesId);
    }

    @Override
    @Transactional
    public void deleteDeadline(Long eventId, Long mangakaId) {
        DeadlineEvent event = deadlineEventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Error: Deadline not found"));

        if (!event.getMangaSeries().getMangaka().getId().equals(mangakaId)) {
            throw new RuntimeException("Error: You do not have permission to delete this deadline.");
        }
        deadlineEventRepository.delete(event);
    }

    // ==========================================
    // [FE-23] The one-second heartbeat is gated by DEADLINE_SCAN_SECONDS.
    // ==========================================
    @Override
    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void scanAndWarnDeadlines() {
        if (!schedulerGate.shouldRun("deadline-warning", "DEADLINE_SCAN_SECONDS", 60)) return;
        LocalDateTime now = LocalDateTime.now();
        Set<Integer> warningDays = runtimeParameters.integerArrayValue(
                "DEADLINE_WARNING_DAYS", Set.of(), 1, 365);

        for (DeadlineEvent event : deadlineEventRepository.findAll()) {
            if ("TRIGGERED".equalsIgnoreCase(event.getWarningLevel())) continue;
            if (event.getDeadlineDate().isAfter(now)) {
                sendAdvanceWarningIfDue(event, now, warningDays);
                continue;
            }
            Long mangakaId = event.getMangaSeries().getMangaka().getId();
            String seriesTitle = event.getMangaSeries().getTitle();

            // 2. Compose the warning message.
            String warningMsg = "🚨 [URGENT DEADLINE] The project '" + seriesTitle + "' has an overdue event: " + event.getEventName() + "!";

            // 3. Send the notification through the WebSocket-enabled notification service.
            notificationService.createNotification(mangakaId, warningMsg);
            if (event.getEventName() != null
                    && event.getEventName().startsWith("[SYSTEM REVIEW]")
                    && event.getMangaSeries().getTantou() != null
                    && !event.getMangaSeries().getTantou().getId().equals(mangakaId)) {
                notificationService.createNotification(
                        event.getMangaSeries().getTantou().getId(), warningMsg);
            }

            // 4. Mark the event TRIGGERED so the next run does not send a duplicate warning.
            event.setWarningLevel("TRIGGERED");
            deadlineEventRepository.save(event);
            
            System.out.println(">>> [CRON JOB] Fired deadline warning for event: " + event.getEventName());
        }
    }

    private void sendAdvanceWarningIfDue(DeadlineEvent event, LocalDateTime now, Set<Integer> warningDays) {
        if (warningDays.isEmpty()) return;
        long remainingSeconds = Duration.between(now, event.getDeadlineDate()).getSeconds();
        Integer nextWarning = warningDays.stream()
                .filter(days -> remainingSeconds <= days * 86_400L)
                .min(Comparator.naturalOrder())
                .orElse(null);
        if (nextWarning == null) return;

        Integer alreadyWarned = warningMarker(event.getWarningLevel());
        if (alreadyWarned != null && nextWarning >= alreadyWarned) return;

        MangaSeries series = event.getMangaSeries();
        String warningMsg = "Deadline reminder: '" + series.getTitle() + "' has "
                + nextWarning + " day" + (nextWarning == 1 ? "" : "s")
                + " remaining for " + event.getEventName() + ".";
        notificationService.createNotification(series.getMangaka().getId(), warningMsg);
        if (event.getEventName() != null && event.getEventName().startsWith("[SYSTEM REVIEW]")
                && series.getTantou() != null
                && !series.getTantou().getId().equals(series.getMangaka().getId())) {
            notificationService.createNotification(series.getTantou().getId(), warningMsg);
        }
        event.setWarningLevel("WARNING_" + nextWarning);
        deadlineEventRepository.save(event);
    }

    private Integer warningMarker(String warningLevel) {
        if (warningLevel == null || !warningLevel.startsWith("WARNING_")) return null;
        try {
            return Integer.parseInt(warningLevel.substring("WARNING_".length()));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
