package com.mangastudio.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "pages", uniqueConstraints = @UniqueConstraint(
        name = "uk_pages_chapter_page_number",
        columnNames = {"chapter_id", "page_number"}
)) // Mỗi số trang chỉ xuất hiện một lần trong cùng chapter.
@EntityListeners(com.mangastudio.backend.listener.PageVersioningListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Page {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;

    @Column(name = "page_number", nullable = false)
    private Integer pageNumber;

    @Column(name = "image_url", length = 255, nullable = false)
    private String imageUrl;

    private Double width;
    
    private Double height;

    // BỔ SUNG: Mối quan hệ để lấy ra toàn bộ lịch sử của trang này
    @OneToMany(mappedBy = "page", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<PageVersion> versions;

    @OneToMany(mappedBy = "page", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Hitbox> hitboxes;

    @OneToMany(mappedBy = "page", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<TantouFeedback> feedbacks;
}
