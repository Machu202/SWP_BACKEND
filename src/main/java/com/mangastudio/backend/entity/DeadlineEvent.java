package com.mangastudio.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Deadline_Event")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DeadlineEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "series_id", nullable = false)
    private MangaSeries mangaSeries;

    @Column(name = "event_name", nullable = false, length = 255)
    private String eventName;

    @Column(name = "deadline_date", nullable = false)
    private LocalDateTime deadlineDate;

    @Column(name = "warning_level", length = 50)
    private String warningLevel;
}