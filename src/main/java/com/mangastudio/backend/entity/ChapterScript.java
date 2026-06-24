package com.mangastudio.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Chapter_Script")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChapterScript {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Quan hệ 1-1: Mỗi Chapter chỉ có 1 Kịch bản
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false, unique = true)
    private Chapter chapter;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}