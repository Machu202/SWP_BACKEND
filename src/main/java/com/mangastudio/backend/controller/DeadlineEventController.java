package com.mangastudio.backend.controller;

import com.mangastudio.backend.entity.DeadlineEvent;
import com.mangastudio.backend.security.UserDetailsImpl;
import com.mangastudio.backend.service.DeadlineEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/deadlines")
@RequiredArgsConstructor
public class DeadlineEventController {

    private final DeadlineEventService deadlineEventService;

    @PostMapping("/series/{seriesId}")
    public ResponseEntity<DeadlineEvent> createDeadline(
        @PathVariable Long seriesId,
        @RequestParam String eventName,
        @RequestParam String deadlineDateStr,
        @RequestParam(defaultValue = "NORMAL") String warningLevel, // <-- Thêm dòng này
        Authentication authentication) {

    UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
    LocalDateTime deadlineDate = LocalDateTime.parse(deadlineDateStr);

    DeadlineEvent event = deadlineEventService.createDeadline(
            seriesId, userDetails.getId(), eventName, deadlineDate, warningLevel);
    return ResponseEntity.status(HttpStatus.CREATED).body(event);
    }

    @GetMapping("/series/{seriesId}")
    public ResponseEntity<List<DeadlineEvent>> getDeadlines(@PathVariable Long seriesId) {
        return ResponseEntity.ok(deadlineEventService.getDeadlinesBySeries(seriesId));
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<String> deleteDeadline(
            @PathVariable Long eventId,
            Authentication authentication) {
        
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        deadlineEventService.deleteDeadline(eventId, userDetails.getId());
        return ResponseEntity.ok("Deadline event deleted successfully.");
    }
}