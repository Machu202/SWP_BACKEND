package com.mangastudio.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Resource")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "resource_type", length = 50)
    private String resourceType;

    @Column(name = "file_url", nullable = false, length = 255)
    private String fileUrl;

    // [BỔ SUNG] Lưu ID của file trên Cloudinary để sau này gọi API xóa
    @Column(name = "public_id", length = 255)
    private String publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;
}