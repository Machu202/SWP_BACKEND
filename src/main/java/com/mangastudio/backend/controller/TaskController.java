package com.mangastudio.backend.controller;

import com.mangastudio.backend.entity.Chapter;
import com.mangastudio.backend.entity.Hitbox;
import com.mangastudio.backend.entity.MangaSeries;
import com.mangastudio.backend.entity.Page;
import com.mangastudio.backend.entity.Task;
import com.mangastudio.backend.entity.User;
import com.mangastudio.backend.security.UserDetailsImpl;
import com.mangastudio.backend.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    // [FE-47] Lọc công việc của bản thân theo Role (Tự động nhận diện qua Token)
    @GetMapping("/my-tasks")
    public ResponseEntity<List<Map<String, Object>>> getMyTasks(Authentication authentication) {
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
        return ResponseEntity.ok(toTaskResponses(tasks));
    }

    // Mangaka gán Assistant vào Task
    @PatchMapping("/{taskId}/assign")
    public ResponseEntity<Map<String, Object>> assignAssistant(
            @PathVariable Long taskId,
            @RequestParam Long assistantId,
            Authentication authentication) {
        
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Task updated = taskService.assignAssistantToTask(taskId, userDetails.getId(), assistantId);
        return ResponseEntity.ok(toTaskResponse(updated));
    }

    // [FE-39] Kéo thả thẻ Task trên bảng Kanban
    @PatchMapping("/{taskId}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @PathVariable Long taskId,
            @RequestParam String newStatus,
            Authentication authentication) {
        
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Task updated = taskService.updateTaskStatus(taskId, userDetails.getId(), newStatus);
        return ResponseEntity.ok(toTaskResponse(updated));
    }

    // Assistant nộp bài (Truyền lên link ảnh đã lưu từ Cloudinary)
    @PatchMapping("/{taskId}/submit")
    public ResponseEntity<Map<String, Object>> submitWork(
            @PathVariable Long taskId,
            @RequestParam String imageUrl,
            Authentication authentication) {
        
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Task updated = taskService.submitTaskWork(taskId, userDetails.getId(), imageUrl);
        return ResponseEntity.ok(toTaskResponse(updated));
    }

    @GetMapping("/series/{seriesId}")
    @Operation(summary = "Get tasks by manga series (For a specific Kanban board)")
    public ResponseEntity<List<Map<String, Object>>> getTasksBySeries(@PathVariable Long seriesId) {
        return ResponseEntity.ok(toTaskResponses(taskService.getTasksBySeries(seriesId)));
    }

    private List<Map<String, Object>> toTaskResponses(List<Task> tasks) {
        return tasks.stream().map(this::toTaskResponse).collect(Collectors.toList());
    }

    private Map<String, Object> toTaskResponse(Task task) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", task.getId());
        dto.put("description", task.getDescription());
        dto.put("status", task.getStatus());
        dto.put("submittedImageUrl", task.getSubmittedImageUrl());
        dto.put("submitted_image_url", task.getSubmittedImageUrl());
        dto.put("createdAt", task.getCreatedAt());
        dto.put("created_at", task.getCreatedAt());

        User mangaka = task.getMangaka();
        if (mangaka != null) {
            dto.put("mangakaId", mangaka.getId());
            dto.put("mangaka_id", mangaka.getId());
            dto.put("mangakaName", displayName(mangaka));
            dto.put("mangakaUsername", mangaka.getUsername());
        }

        User assistant = task.getAssistant();
        if (assistant != null) {
            dto.put("assistantId", assistant.getId());
            dto.put("assistant_id", assistant.getId());
            dto.put("assistantName", displayName(assistant));
            dto.put("assistantUsername", assistant.getUsername());
            dto.put("assistantEmail", assistant.getEmail());
        }

        Hitbox hitbox = task.getHitbox();
        if (hitbox != null) {
            dto.put("hitboxId", hitbox.getId());
            dto.put("hitbox_id", hitbox.getId());
            dto.put("xCoord", hitbox.getXCoord());
            dto.put("x_coord", hitbox.getXCoord());
            dto.put("yCoord", hitbox.getYCoord());
            dto.put("y_coord", hitbox.getYCoord());
            dto.put("width", hitbox.getWidth());
            dto.put("height", hitbox.getHeight());

            Map<String, Object> hitboxDto = new HashMap<>();
            hitboxDto.put("id", hitbox.getId());
            hitboxDto.put("xCoord", hitbox.getXCoord());
            hitboxDto.put("x_coord", hitbox.getXCoord());
            hitboxDto.put("yCoord", hitbox.getYCoord());
            hitboxDto.put("y_coord", hitbox.getYCoord());
            hitboxDto.put("width", hitbox.getWidth());
            hitboxDto.put("height", hitbox.getHeight());
            dto.put("hitbox", hitboxDto);

            Page page = hitbox.getPage();
            if (page != null) {
                dto.put("pageId", page.getId());
                dto.put("page_id", page.getId());
                dto.put("pageNumber", page.getPageNumber());
                dto.put("page_number", page.getPageNumber());
                dto.put("pageImageUrl", page.getImageUrl());
                dto.put("page_image_url", page.getImageUrl());
                dto.put("referenceImageUrl", page.getImageUrl());
                dto.put("reference_image_url", page.getImageUrl());
                hitboxDto.put("pageId", page.getId());
                hitboxDto.put("page_id", page.getId());
                hitboxDto.put("pageNumber", page.getPageNumber());
                hitboxDto.put("page_number", page.getPageNumber());
                hitboxDto.put("pageImageUrl", page.getImageUrl());
                hitboxDto.put("page_image_url", page.getImageUrl());

                Chapter chapter = page.getChapter();
                if (chapter != null) {
                    dto.put("chapterId", chapter.getId());
                    dto.put("chapter_id", chapter.getId());
                    dto.put("chapterNumber", chapter.getChapterNumber());
                    dto.put("chapter_number", chapter.getChapterNumber());
                    dto.put("chapterTitle", chapter.getTitle());
                    dto.put("chapter_title", chapter.getTitle());

                    MangaSeries series = chapter.getMangaSeries();
                    if (series != null) {
                        dto.put("seriesId", series.getId());
                        dto.put("series_id", series.getId());
                        dto.put("seriesTitle", series.getTitle());
                        dto.put("series_title", series.getTitle());
                        dto.put("seriesStatus", series.getStatus());
                        dto.put("series_status", series.getStatus());
                    }
                }
            }
        }
        return dto;
    }

    private String displayName(User user) {
        if (user.getFullName() != null && !user.getFullName().isBlank()) return user.getFullName();
        if (user.getUsername() != null && !user.getUsername().isBlank()) return user.getUsername();
        if (user.getEmail() != null && !user.getEmail().isBlank()) return user.getEmail();
        return "User #" + user.getId();
    }
}
