package com.mangastudio.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Task")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Task {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long taskId;

    @Column(name = "page_id", nullable = false)
    private Long pageId;

    @Column(name = "mangaka_id", nullable = false)
    private Long mangakaId; 

    @Column(name = "assistant_id", nullable = false)
    private Long assistantId; 

    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    private TaskStatus status;

    @Column(name = "task_desc", columnDefinition = "TEXT", nullable = false)
    private String taskDesc;

    // Coordinates float (0.0 -> 1.0)
    private Double xNorm;
    private Double yNorm;
    private Double widthNorm;
    private Double heightNorm;

    @Column(name = "submitted_image_url", length = 500)
    private String submittedImageUrl; 

    private LocalDateTime deadline;

    public enum TaskStatus { TODO, DOING, REVIEWING, REJECTED, APPROVED }
}