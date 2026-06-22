package com.mangastudio.backend.controller;

import com.mangastudio.backend.dto.CanvasProportionResponse;
import com.mangastudio.backend.dto.CreateTaskRequest;
import com.mangastudio.backend.dto.TaskResponse;
import com.mangastudio.backend.service.WorkspaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/workspace/pages")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @GetMapping("/{pageId}/canvas-proportions")
    public ResponseEntity<CanvasProportionResponse> getProportions(
            @PathVariable Long pageId,
            @RequestParam(defaultValue = "1000") int containerWidth) {
        return ResponseEntity.ok(workspaceService.getCanvasProportions(pageId, containerWidth));
    }

    @PostMapping("/{pageId}/tasks")
    public ResponseEntity<TaskResponse> createAndAssignTask(
            @PathVariable Long pageId,
            @RequestHeader("X-User-Id") Long mangakaId, 
            @Valid @RequestBody CreateTaskRequest request) {
        return ResponseEntity.ok(workspaceService.assignTaskToAssistant(pageId, mangakaId, request));
    }
}