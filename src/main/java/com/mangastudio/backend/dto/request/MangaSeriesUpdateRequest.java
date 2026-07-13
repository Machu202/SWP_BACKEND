package com.mangastudio.backend.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MangaSeriesUpdateRequest {
    private String title;
    private String genre;
    private String summary;
    private String description;

    @JsonAlias({"cover_image_url", "coverUrl", "cover_url", "imageUrl", "image_url", "thumbnailUrl", "thumbnail_url"})
    private String coverImageUrl;
}
