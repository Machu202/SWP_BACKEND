package com.mangastudio.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Editor_Annotation")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EditorAnnotation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long annotationId;

    @Column(name = "page_id", nullable = false)
    private Long pageId;

    @Column(name = "editor_id", nullable = false)
    private Long editorId;

    @Column(nullable = false) private Double xNorm;
    @Column(nullable = false) private Double yNorm;
    @Column(nullable = false) private Double widthNorm;
    @Column(nullable = false) private Double heightNorm;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String comment;

    @Column(name = "is_resolved", nullable = false)
    @Builder.Default
    private Boolean isResolved = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}