package com.mangastudio.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Page_Version")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PageVersion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long versionId;

    @Column(name = "page_id", nullable = false)
    private Long pageId;

    @Column(name = "version_tag", length = 50, nullable = false)
    private String versionTag; 

    @Column(name = "commit_message")
    private String commitMessage;

    @Column(name = "old_image_url", length = 500, nullable = false)
    private String oldImageUrl;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}