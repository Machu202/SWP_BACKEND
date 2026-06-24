package com.mangastudio.backend.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MangaSeriesUpdateRequest {
    private String title;
    private String genre;
    private String summary;
}