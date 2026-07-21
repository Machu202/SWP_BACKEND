package com.mangastudio.backend.controller;

import org.springframework.data.domain.Page;
import com.mangastudio.backend.dto.request.MangaSeriesCreateRequest;
import com.mangastudio.backend.dto.request.MangaSeriesUpdateRequest;
import com.mangastudio.backend.dto.response.MangaSeriesResponse;
import com.mangastudio.backend.entity.MangaSeries;
import com.mangastudio.backend.entity.PublishingSchedule;
import com.mangastudio.backend.security.UserDetailsImpl;
import com.mangastudio.backend.service.MangaSeriesService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.format.annotation.DateTimeFormat;
import io.swagger.v3.oas.annotations.Operation;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/manga-series")
public class MangaSeriesController {

    private final MangaSeriesService mangaSeriesService;

    public MangaSeriesController(MangaSeriesService mangaSeriesService) {
        this.mangaSeriesService = mangaSeriesService;
    }

    @PostMapping
    @PreAuthorize("hasRole('MANGAKA')")
    public ResponseEntity<MangaSeriesResponse> createSeries(
            Authentication authentication,
            @Valid @RequestBody MangaSeriesCreateRequest request) {
        
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Long currentUserId = userDetails.getId();
        
        MangaSeriesResponse response = mangaSeriesService.createSeries(currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my-series")
    public ResponseEntity<List<MangaSeriesResponse>> getMySeries(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Long currentUserId = userDetails.getId();
        
        List<MangaSeriesResponse> responses = mangaSeriesService.getAllSeriesByMangaka(currentUserId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/assigned-to-me")
    @PreAuthorize("hasAuthority('ROLE_TANTOU EDITOR')")
    @Operation(summary = "Get only manga series assigned to the authenticated Tantou Editor")
    public ResponseEntity<List<MangaSeriesResponse>> getAssignedSeries(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return ResponseEntity.ok(mangaSeriesService.getAllSeriesAssignedToTantou(userDetails.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MangaSeriesResponse> getSeriesById(@PathVariable Long id) {
        MangaSeriesResponse response = mangaSeriesService.getSeriesById(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('MANGAKA')")
    public ResponseEntity<MangaSeriesResponse> updateSeriesStatus(
            @PathVariable Long id,
            @RequestParam String newStatus,
            Authentication authentication) {

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        MangaSeriesResponse response = mangaSeriesService.updateSeriesStatus(id, userDetails.getId(), newStatus);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/publication-schedule")
    @PreAuthorize("hasRole('MANGAKA')")
    @Operation(summary = "Schedule an approved manga series and its first publicable chapter for launch")
    public ResponseEntity<PublishingSchedule> schedulePublication(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime publishAt,
            Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mangaSeriesService.schedulePublication(id, userDetails.getId(), publishAt));
    }

    @PatchMapping("/{id}/tantou")
    @PreAuthorize("hasAnyRole('MANGAKA', 'ADMIN')")
    @Operation(summary = "Assign a Tantou Editor to a manga series before chapter review")
    public ResponseEntity<MangaSeriesResponse> assignTantou(
            @PathVariable Long id,
            @RequestParam Long tantouId,
            Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return ResponseEntity.ok(mangaSeriesService.assignTantou(id, userDetails.getId(), tantouId));
    }

    @PatchMapping("/{id}/submit-to-board")
    @PreAuthorize("hasAuthority('ROLE_TANTOU EDITOR')")
    @Operation(summary = "Assigned Tantou submits a fully approved series to the Editorial Board")
    public ResponseEntity<MangaSeriesResponse> submitToEditorialBoard(
            @PathVariable Long id,
            Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return ResponseEntity.ok(mangaSeriesService.submitToEditorialBoard(id, userDetails.getId()));
    }

    // Updates project metadata.
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANGAKA')")
    public ResponseEntity<MangaSeriesResponse> updateSeriesMetadata(
            @PathVariable Long id,
            Authentication authentication,
            @RequestBody MangaSeriesUpdateRequest request) {
        
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Long currentUserId = userDetails.getId();
        
        MangaSeriesResponse response = mangaSeriesService.updateSeriesMetadata(id, currentUserId, request);
        return ResponseEntity.ok(response);
    }

    // Deletes a draft project.
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANGAKA')")
    public ResponseEntity<String> deleteSeries(
            @PathVariable Long id,
            Authentication authentication) {
        
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Long currentUserId = userDetails.getId();
        
        mangaSeriesService.deleteSeries(id, currentUserId);
        return ResponseEntity.ok("Manga Series deleted successfully.");
    }
    
    @PatchMapping("/{id}/admin-decision")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin approves or rejects a manga series. If approved, optionally assign a Tantou.")
    public ResponseEntity<MangaSeries> handleAdminDecision(
            @PathVariable Long id,
            @RequestParam Boolean isApproved,
            @RequestParam(required = false) Long tantouId // Optional when the Admin rejects the series.
    ) {
        MangaSeries updatedSeries = mangaSeriesService.handleAdminDecision(id, isApproved, tantouId);
        return ResponseEntity.ok(updatedSeries);
    }

    @GetMapping
    @Operation(summary = "Filter manga series by status with pagination (e.g., ?status=REVIEWING&page=0&size=20)")
    public ResponseEntity<Page<MangaSeriesResponse>> getSeriesByFilter(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<MangaSeriesResponse> responsePage = mangaSeriesService.getSeriesByStatus(status, page, size);
        return ResponseEntity.ok(responsePage);
    }
}
