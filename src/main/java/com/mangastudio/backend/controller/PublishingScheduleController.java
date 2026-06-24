package com.mangastudio.backend.controller;

import com.mangastudio.backend.dto.request.PublishingScheduleRequest;
import com.mangastudio.backend.entity.PublishingSchedule;
import com.mangastudio.backend.security.UserDetailsImpl;
import com.mangastudio.backend.service.PublishingScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
public class PublishingScheduleController {

    private final PublishingScheduleService scheduleService;

    @PostMapping
    public ResponseEntity<PublishingSchedule> createSchedule(
            Authentication authentication,
            @Valid @RequestBody PublishingScheduleRequest request) {
        
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        PublishingSchedule response = scheduleService.createSchedule(userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/series/{seriesId}")
    public ResponseEntity<List<PublishingSchedule>> getSchedulesBySeries(@PathVariable Long seriesId) {
        return ResponseEntity.ok(scheduleService.getSchedulesBySeries(seriesId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PublishingSchedule> updateSchedule(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody PublishingScheduleRequest request) {
        
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        PublishingSchedule response = scheduleService.updateSchedule(id, userDetails.getId(), request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteSchedule(
            @PathVariable Long id,
            Authentication authentication) {
        
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        scheduleService.deleteSchedule(id, userDetails.getId());
        return ResponseEntity.ok("Publishing schedule deleted successfully.");
    }
}