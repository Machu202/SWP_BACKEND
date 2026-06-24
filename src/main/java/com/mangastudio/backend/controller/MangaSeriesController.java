package com.mangastudio.backend.controller;

import com.mangastudio.backend.dto.request.MangaSeriesCreateRequest;
import com.mangastudio.backend.dto.response.MangaSeriesResponse;
import com.mangastudio.backend.security.UserDetailsImpl;
import com.mangastudio.backend.service.MangaSeriesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/manga-series")
@RequiredArgsConstructor
public class MangaSeriesController {

    private final MangaSeriesService mangaSeriesService;

    // Mangaka tạo dự án truyện mới
    @PostMapping
    public ResponseEntity<MangaSeriesResponse> createSeries(
            Authentication authentication,
            @Valid @RequestBody MangaSeriesCreateRequest request) {
        
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Long currentUserId = userDetails.getId();
        
        MangaSeriesResponse response = mangaSeriesService.createSeries(currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Lấy danh sách truyện do mình làm tác giả
    @GetMapping("/my-series")
    public ResponseEntity<List<MangaSeriesResponse>> getMySeries(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Long currentUserId = userDetails.getId();
        
        List<MangaSeriesResponse> responses = mangaSeriesService.getAllSeriesByMangaka(currentUserId);
        return ResponseEntity.ok(responses);
    }

    // Lấy chi tiết 1 bộ truyện theo ID
    @GetMapping("/{id}")
    public ResponseEntity<MangaSeriesResponse> getSeriesById(@PathVariable Long id) {
        MangaSeriesResponse response = mangaSeriesService.getSeriesById(id);
        return ResponseEntity.ok(response);
    }

    // Đổi trạng thái bộ truyện (ví dụ: Draft -> Reviewing)
    @PatchMapping("/{id}/status")
    public ResponseEntity<MangaSeriesResponse> updateSeriesStatus(
            @PathVariable Long id,
            @RequestParam String newStatus) {
        
        MangaSeriesResponse response = mangaSeriesService.updateSeriesStatus(id, newStatus);
        return ResponseEntity.ok(response);
    }
}