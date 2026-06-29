package com.mangastudio.backend.controller;

import com.mangastudio.backend.entity.ChapterScript;
import com.mangastudio.backend.security.UserDetailsImpl;
import com.mangastudio.backend.service.ChapterScriptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
@RestController
@RequestMapping("/api/v1/chapter-scripts")
@RequiredArgsConstructor
public class ChapterScriptController {

    private final ChapterScriptService scriptService;

    @GetMapping("/chapters/{chapterId}")
    public ResponseEntity<ChapterScript> getScript(@PathVariable Long chapterId) {
        return ResponseEntity.ok(scriptService.getScriptByChapter(chapterId));
    }

    @PostMapping("/chapters/{chapterId}")
    public ResponseEntity<ChapterScript> saveScript(
            @PathVariable Long chapterId,
            @RequestBody String content,
            Authentication authentication) {
        
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return ResponseEntity.ok(scriptService.saveOrUpdateScript(chapterId, userDetails.getId(), content));
    }

    @GetMapping("/series/{seriesId}")
    @Operation(summary = "Lấy danh sách toàn bộ kịch bản các chương của một bộ truyện")
    public ResponseEntity<List<ChapterScript>> getScriptsBySeries(@PathVariable Long seriesId) {
        return ResponseEntity.ok(scriptService.getScriptsBySeries(seriesId));
    }
}