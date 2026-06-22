package com.mangastudio.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Page")
@EntityListeners(com.mangastudio.backend.listener.PageVersioningListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Page {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long pageId;

    @Column(name = "chapter_id", nullable = false)
    private Long chapterId; 

    @Column(name = "page_number", nullable = false)
    private Integer pageNumber;

    @Column(name = "image_url", length = 500, nullable = false)
    private String imageUrl;

    private Double width;
    private Double height;
}