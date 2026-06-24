package com.mangastudio.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Manga_Series")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MangaSeries {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mangaka_id", nullable = false)
    private User mangaka;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tantou_id") // Có thể null nếu dự án mới chưa được phân công Tantou
    private User tantou;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 100)
    private String genre;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String summary;

    @Column(length = 50)
    private String status;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}