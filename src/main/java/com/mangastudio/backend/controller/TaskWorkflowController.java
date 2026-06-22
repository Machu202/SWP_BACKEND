package com.mangastudio.backend.controller;

import com.mangastudio.backend.dto.TaskResponse;
import com.mangastudio.backend.service.TaskWorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/workspace/tasks")
@RequiredArgsConstructor
public class TaskWorkflowController {

    private final TaskWorkflowService workflowService;

    @PutMapping("/{taskId}/submit")
    public ResponseEntity<TaskResponse> submitWork(
            @PathVariable Long taskId,
            @RequestHeader("X-User-Id") Long assistantId,
            @RequestParam String resultImageUrl) {
        return ResponseEntity.ok(workflowService.submitAssistantWork(taskId, assistantId, resultImageUrl));
    }

    @PutMapping("/{taskId}/review")
    public ResponseEntity<TaskResponse> reviewWork(
            @PathVariable Long taskId,
            @RequestHeader("X-User-Id") Long mangakaId,
            @RequestParam boolean isApproved) {
        return ResponseEntity.ok(workflowService.reviewTaskByMangaka(taskId, mangakaId, isApproved));
    }
}