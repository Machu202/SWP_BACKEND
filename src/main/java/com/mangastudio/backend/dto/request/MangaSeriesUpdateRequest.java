package com.mangastudio.backend.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MangaSeriesUpdateRequest {
    private String title;
    private String genre;
    private String summary;
    private String description;
    private String coverImageUrl;
    private String coverUrl;
    private String imageUrl;
    private String thumbnailUrl;
    private String coverImage;
    private String primaryArtUrl;
}