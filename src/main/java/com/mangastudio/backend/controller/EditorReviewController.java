package com.mangastudio.backend.controller;

import com.mangastudio.backend.dto.CreateEditorAnnotationRequest;
import com.mangastudio.backend.entity.EditorAnnotation;
import com.mangastudio.backend.service.EditorReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/editor/pages")
@RequiredArgsConstructor
public class EditorReviewController {

    private final EditorReviewService reviewService;

    @PostMapping("/{pageId}/annotations")
    public ResponseEntity<EditorAnnotation> pinError(
            @PathVariable Long pageId,
            @Valid @RequestBody CreateEditorAnnotationRequest request) {
        return ResponseEntity.ok(reviewService.pinAnnotation(pageId, request));
    }

    @GetMapping("/{pageId}/annotations")
    public ResponseEntity<List<EditorAnnotation>> getPageErrors(@PathVariable Long pageId) {
        return ResponseEntity.ok(reviewService.getAnnotationsByPage(pageId));
    }

    @PutMapping("/annotations/{annotationId}/resolve")
    public ResponseEntity<Void> resolveError(@PathVariable Long annotationId) {
        reviewService.resolveAnnotation(annotationId);
        return ResponseEntity.ok().build();
    }
}