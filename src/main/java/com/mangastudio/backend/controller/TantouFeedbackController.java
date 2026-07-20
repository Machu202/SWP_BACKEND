package com.mangastudio.backend.controller;

import com.mangastudio.backend.entity.TantouFeedback;
import com.mangastudio.backend.entity.Task;
import com.mangastudio.backend.security.UserDetailsImpl;
import com.mangastudio.backend.service.TantouFeedbackService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tantou-feedbacks")
public class TantouFeedbackController {

    private final TantouFeedbackService tantouFeedbackService;

    public TantouFeedbackController(TantouFeedbackService tantouFeedbackService) {
        this.tantouFeedbackService = tantouFeedbackService;
    }

    @PostMapping("/pages/{pageId}")
    public ResponseEntity<TantouFeedback> createFeedback(
            @PathVariable Long pageId,
            @RequestParam Double x,
            @RequestParam Double y,
            @RequestParam Double width,
            @RequestParam Double height,
            @RequestParam String content,
            Authentication authentication) {
        
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        TantouFeedback feedback = tantouFeedbackService.createFeedback(pageId, userDetails.getId(), x, y, width, height, content);
        return ResponseEntity.status(HttpStatus.CREATED).body(feedback);
    }

    @GetMapping("/pages/{pageId}")
    public ResponseEntity<List<TantouFeedback>> getPageFeedbacks(
            @PathVariable Long pageId,
            Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return ResponseEntity.ok(tantouFeedbackService.getFeedbacksByPage(pageId, userDetails.getId()));
    }

    @PostMapping("/{feedbackId}/comments")
    public ResponseEntity<TantouFeedback> addMangakaComment(
            @PathVariable Long feedbackId,
            @RequestParam String content,
            Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        TantouFeedback comment = tantouFeedbackService.addMangakaComment(feedbackId, userDetails.getId(), content);
        return ResponseEntity.status(HttpStatus.CREATED).body(comment);
    }

    @PostMapping("/{feedbackId}/assistant-task")
    public ResponseEntity<Task> createAssistantTask(
            @PathVariable Long feedbackId,
            @RequestParam Long assistantId,
            Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Task task = tantouFeedbackService.createAssistantTask(feedbackId, userDetails.getId(), assistantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(task);
    }

    @PatchMapping("/{feedbackId}/resolve")
    public ResponseEntity<TantouFeedback> resolveFeedback(
            @PathVariable Long feedbackId,
            Authentication authentication) {
        
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return ResponseEntity.ok(tantouFeedbackService.resolveFeedback(feedbackId, userDetails.getId()));
    }
}
