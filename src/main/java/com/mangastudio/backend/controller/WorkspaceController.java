package com.mangastudio.backend.controller;

import com.mangastudio.backend.dto.response.CanvasInitResponse;
import com.mangastudio.backend.entity.Hitbox;
import com.mangastudio.backend.entity.Task;
import com.mangastudio.backend.security.UserDetailsImpl;
import com.mangastudio.backend.service.WorkspaceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workspace")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @PostMapping("/pages/{pageId}/hitboxes")
    public ResponseEntity<Hitbox> createHitbox(
            @PathVariable Long pageId,
            @RequestParam Double x,
            @RequestParam Double y,
            @RequestParam Double width,
            @RequestParam Double height,
            Authentication authentication) {
        
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Long currentUserId = userDetails.getId();

        Hitbox createdHitbox = workspaceService.createHitbox(pageId, currentUserId, x, y, width, height);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdHitbox);
    }

    @GetMapping("/pages/{pageId}/hitboxes")
    public ResponseEntity<List<Hitbox>> getHitboxesByPage(@PathVariable Long pageId) {
        return ResponseEntity.ok(workspaceService.getHitboxesByPage(pageId));
    }

    @DeleteMapping("/hitboxes/{hitboxId}")
    public ResponseEntity<String> deleteHitbox(
            @PathVariable Long hitboxId,
            Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        workspaceService.deleteHitbox(hitboxId, userDetails.getId());
        return ResponseEntity.ok("Hitbox deleted successfully.");
    }

    @PostMapping("/hitboxes/{hitboxId}/task")
    public ResponseEntity<Task> assignTask(
            @PathVariable Long hitboxId,
            @RequestBody Task taskRequest,
            Authentication authentication) {
        
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Long mangakaId = userDetails.getId();

        Task assignedTask = workspaceService.assignTaskToHitbox(hitboxId, mangakaId, taskRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(assignedTask);
    }
    // [FE-33] All-in-one endpoint used to initialize the Canvas UI.
    @GetMapping("/pages/{pageId}/canvas-init")
    public ResponseEntity<CanvasInitResponse> initCanvas(@PathVariable Long pageId) {
        return ResponseEntity.ok(workspaceService.getCanvasInitData(pageId));
    }
}
