package com.mangastudio.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Telemetry_Analytics")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TelemetryAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "series_id", nullable = false)
    private MangaSeries mangaSeries;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by", nullable = false)
    private User recordedBy; // Record owner

    @Column(name = "reader_votes")
    private Integer readerVotes;

    @Column(name = "views")
    private Integer views;

    @Column(name = "calculated_at")
    @Builder.Default
    private LocalDateTime calculatedAt = LocalDateTime.now();
}
