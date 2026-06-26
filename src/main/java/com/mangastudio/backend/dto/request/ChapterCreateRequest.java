package com.mangastudio.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChapterCreateRequest {
    
    @NotNull(message = "Series ID cannot be null")
    private Long seriesId;

    @NotNull(message = "Chapter number cannot be null")
    private Integer chapterNumber;

    @NotBlank(message = "Title cannot be blank")
    private String title;
}