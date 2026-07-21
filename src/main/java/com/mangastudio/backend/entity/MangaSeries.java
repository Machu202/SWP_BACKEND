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
    @JoinColumn(name = "tantou_id") // Null until a Tantou Editor is assigned to a new project.
    private User tantou;
    public User getTantou() { return tantou; }
    public void setTantou(User tantou) { this.tantou = tantou; }
    
    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 100)
    private String genre;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "cover_image_url", length = 1000)
    private String coverImageUrl;

    @Column(length = 50)
    private String status;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
