package com.mangastudio.backend.controller;

import com.mangastudio.backend.dto.response.TaskResponse;
import com.mangastudio.backend.entity.Task;
import com.mangastudio.backend.security.UserDetailsImpl;
import com.mangastudio.backend.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    // [FE-47] Lọc công việc của bản thân theo Role (Tự động nhận diện qua Token)
    @GetMapping("/my-tasks")
    public ResponseEntity<List<TaskResponse>> getMyTasks(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Long userId = userDetails.getId();

        String role = userDetails.getAuthorities().iterator().next().getAuthority();

        List<Task> tasks;
        if (role.equals("ROLE_MANGAKA")) {
            tasks = taskService.getTasksByMangaka(userId);
        } else {
            // Mặc định các Role khác (Chủ yếu là Assistant)
            tasks = taskService.getTasksByAssistant(userId);
        }

        return ResponseEntity.ok(toResponseList(tasks));
    }

    // Mangaka gán Assistant vào Task
    @PatchMapping("/{taskId}/assign")
    public ResponseEntity<TaskResponse> assignAssistant(
            @PathVariable Long taskId,
            @RequestParam Long assistantId,
            Authentication authentication) {

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return ResponseEntity.ok(TaskResponse.from(
                taskService.assignAssistantToTask(taskId, userDetails.getId(), assistantId)
        ));
    }

    // [FE-39] Kéo thả thẻ Task trên bảng Kanban
    @PatchMapping("/{taskId}/status")
    public ResponseEntity<TaskResponse> updateStatus(
            @PathVariable Long taskId,
            @RequestParam String newStatus,
            Authentication authentication) {

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return ResponseEntity.ok(TaskResponse.from(
                taskService.updateTaskStatus(taskId, userDetails.getId(), newStatus)
        ));
    }

    // Assistant nộp bài (Truyền lên link ảnh đã lưu từ Cloudinary)
    @PatchMapping("/{taskId}/submit")
    public ResponseEntity<TaskResponse> submitWork(
            @PathVariable Long taskId,
            @RequestParam String imageUrl,
            Authentication authentication) {

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return ResponseEntity.ok(TaskResponse.from(
                taskService.submitTaskWork(taskId, userDetails.getId(), imageUrl)
        ));
    }

    @GetMapping("/series/{seriesId}")
    @Operation(summary = "Get tasks by manga series (For a specific Kanban board)")
    public ResponseEntity<List<TaskResponse>> getTasksBySeries(@PathVariable Long seriesId) {
        return ResponseEntity.ok(toResponseList(taskService.getTasksBySeries(seriesId)));
    }

    private List<TaskResponse> toResponseList(List<Task> tasks) {
        return tasks.stream()
                .map(TaskResponse::from)
                .collect(Collectors.toList());
    }
}
