package com.mangastudio.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Chapter")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Chapter {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "series_id", nullable = false)
    private MangaSeries mangaSeries;

    @Column(name = "chapter_number", nullable = false)
    private Integer chapterNumber;

    @Column(length = 255)
    private String title;

    @Column(name = "publish_status", length = 50)
    private String publishStatus;
}