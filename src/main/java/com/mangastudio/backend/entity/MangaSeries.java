package com.mangastudio.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "manga_series")
@Data
public class MangaSeries {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "series_id")
    private Long seriesId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 100)
    private String genre;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(length = 50)
    private String status;

    // ==========================================
    // FOREIGN KEYS (RELATIONSHIPS)
    // ==========================================
    
    // Links to the User table for the Main Author (Mangaka)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mangaka_id", nullable = false)
    private User mangaka;

    // Links to the User table for the Assigned Editor (Tantou Editor)
    // This is nullable because a draft might not have a Tantou assigned yet
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tantou_id", nullable = true)
    private User tantou;
}