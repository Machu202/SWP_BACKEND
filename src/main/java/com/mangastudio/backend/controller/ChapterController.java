package com.mangastudio.backend.controller;

import com.mangastudio.backend.dto.request.ChapterCreateRequest;
import com.mangastudio.backend.dto.response.ChapterResponse;
import com.mangastudio.backend.security.UserDetailsImpl;
import com.mangastudio.backend.service.ChapterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chapters")
@RequiredArgsConstructor
public class ChapterController {

    private final ChapterService chapterService;

    @PostMapping
    public ResponseEntity<ChapterResponse> createChapter(
            Authentication authentication,
            @Valid @RequestBody ChapterCreateRequest request) {
        
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        ChapterResponse response = chapterService.createChapter(userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChapterResponse> getChapterById(@PathVariable Long id) {
        return ResponseEntity.ok(chapterService.getChapterById(id));
    }

    @GetMapping("/series/{seriesId}")
    public ResponseEntity<List<ChapterResponse>> getAllChaptersBySeries(@PathVariable Long seriesId) {
        return ResponseEntity.ok(chapterService.getAllChaptersBySeries(seriesId));
    }

    // [FE-30] API Cập nhật trạng thái
    @PatchMapping("/{id}/status")
    public ResponseEntity<ChapterResponse> updateChapterStatus(
            @PathVariable Long id,
            @RequestParam String newStatus,
            Authentication authentication) {
        
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        ChapterResponse response = chapterService.updateChapterStatus(id, userDetails.getId(), newStatus);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteChapter(
            @PathVariable Long id,
            Authentication authentication) {
        
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        chapterService.deleteChapter(id, userDetails.getId());
        return ResponseEntity.ok("Chapter deleted successfully.");
    }
}