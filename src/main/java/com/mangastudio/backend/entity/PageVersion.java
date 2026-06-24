package com.mangastudio.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "page_versions")
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class PageVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many versions belong to one specific page
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id", nullable = false)
    private Page page;

    @Column(name = "image_url", length = 255, nullable = false)
    private String imageUrl;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    // Optional: You can add an 'updated_by' field later if you want to track 
    // exactly WHICH assistant or mangaka created this specific version.
}