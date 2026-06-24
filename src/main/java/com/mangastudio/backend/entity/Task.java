package com.mangastudio.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Task")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Quan hệ 1-1 với Hitbox: Mỗi Hitbox là một Task
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hitbox_id", nullable = false, unique = true)
    private Hitbox hitbox;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mangaka_id", nullable = false)
    private User mangaka;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assistant_id") // Có thể null nếu chưa ai nhận
    private User assistant;

    @Column(length = 50)
    private String status; // Todo, Doing, Reviewing, Approved

    @Column(columnDefinition = "NVARCHAR(MAX)", nullable = false)
    private String description;

    @Column(name = "submitted_image_url", length = 255)
    private String submittedImageUrl;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}