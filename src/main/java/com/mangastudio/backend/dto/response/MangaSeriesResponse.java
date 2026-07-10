package com.mangastudio.backend.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class MangaSeriesResponse {
    private Long id;
    private String title;
    private String genre;
    private String summary;
    private String description;
    private String coverImageUrl;
    private String coverUrl;
    private String imageUrl;
    private String thumbnailUrl;
    private String status;
    private Long mangakaId;
    private String mangakaUsername;
    private String mangakaEmail;
    private String mangakaName;
    private String tantouName; // Có thể null
    private LocalDateTime createdAt;
}