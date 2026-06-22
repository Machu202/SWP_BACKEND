package com.mangastudio.backend.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mangastudio.backend.dto.CreateEditorAnnotationRequest;
import com.mangastudio.backend.entity.EditorAnnotation;
import com.mangastudio.backend.repository.EditorAnnotationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EditorReviewService {

    private final EditorAnnotationRepository annotationRepo;

    @Transactional
    public EditorAnnotation pinAnnotation(Long pageId, CreateEditorAnnotationRequest req) {
        EditorAnnotation ann = EditorAnnotation.builder()
                .pageId(pageId)
                .editorId(req.editorId())
                .xNorm(req.xNorm())
                .yNorm(req.yNorm())
                .widthNorm(req.widthNorm())
                .heightNorm(req.heightNorm())
                .comment(req.comment())
                .isResolved(false)
                .createdAt(LocalDateTime.now())
                .build();

        return annotationRepo.save(ann);
    }

    public List<EditorAnnotation> getAnnotationsByPage(Long pageId) {
        return annotationRepo.findByPageId(pageId);
    }

    @Transactional
    public void resolveAnnotation(Long annotationId) {
        EditorAnnotation ann = annotationRepo.findById(annotationId)
                .orElseThrow(() -> new RuntimeException("Annotation not found"));
        ann.setIsResolved(true);
        annotationRepo.save(ann);
    }
}