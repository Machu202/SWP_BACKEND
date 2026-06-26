package com.mangastudio.backend.controller;

import com.mangastudio.backend.entity.HitboxComment;
import com.mangastudio.backend.security.UserDetailsImpl;
import com.mangastudio.backend.service.HitboxCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/hitbox-comments")
@RequiredArgsConstructor
public class HitboxCommentController {

    private final HitboxCommentService hitboxCommentService;

    @PostMapping("/{hitboxId}")
    public ResponseEntity<HitboxComment> addComment(
            @PathVariable Long hitboxId,
            @RequestParam String content,
            Authentication authentication) {
        
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        HitboxComment comment = hitboxCommentService.addCommentToHitbox(hitboxId, userDetails.getId(), content);
        return ResponseEntity.status(HttpStatus.CREATED).body(comment);
    }

    @GetMapping("/{hitboxId}")
    public ResponseEntity<List<HitboxComment>> getHitboxComments(@PathVariable Long hitboxId) {
        return ResponseEntity.ok(hitboxCommentService.getCommentsByHitbox(hitboxId));
    }
}