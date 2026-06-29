package com.mangastudio.backend.controller;

import org.springframework.data.domain.Page;
import com.mangastudio.backend.dto.request.MangaSeriesCreateRequest;
import com.mangastudio.backend.dto.request.MangaSeriesUpdateRequest;
import com.mangastudio.backend.dto.response.MangaSeriesResponse;
import com.mangastudio.backend.entity.MangaSeries;
import com.mangastudio.backend.security.UserDetailsImpl;
import com.mangastudio.backend.service.MangaSeriesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;

@RestController
@RequestMapping("/api/v1/manga-series")
@RequiredArgsConstructor
public class MangaSeriesController {

    private final MangaSeriesService mangaSeriesService;

    @PostMapping
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

    @GetMapping("/{id}")
    public ResponseEntity<MangaSeriesResponse> getSeriesById(@PathVariable Long id) {
        MangaSeriesResponse response = mangaSeriesService.getSeriesById(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<MangaSeriesResponse> updateSeriesStatus(
            @PathVariable Long id,
            @RequestParam String newStatus) {
        
        MangaSeriesResponse response = mangaSeriesService.updateSeriesStatus(id, newStatus);
        return ResponseEntity.ok(response);
    }

    // [BỔ SUNG] Cập nhật Metadata của dự án
    @PutMapping("/{id}")
    public ResponseEntity<MangaSeriesResponse> updateSeriesMetadata(
            @PathVariable Long id,
            Authentication authentication,
            @RequestBody MangaSeriesUpdateRequest request) {
        
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Long currentUserId = userDetails.getId();
        
        MangaSeriesResponse response = mangaSeriesService.updateSeriesMetadata(id, currentUserId, request);
        return ResponseEntity.ok(response);
    }

    // [BỔ SUNG] Xóa dự án nháp
    @DeleteMapping("/{id}")
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
            @RequestParam(required = false) Long tantouId // required = false để không bắt buộc nếu Admin muốn từ chối (Reject)
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