package com.mangastudio.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MangaSeriesCreateRequest {

    @NotBlank(message = "Title cannot be blank")
    private String title;

    private String genre;
    private String summary;
}