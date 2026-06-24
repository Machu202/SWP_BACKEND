package com.mangastudio.backend.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ChapterResponse {
    private Long id;
    private Long seriesId;
    private String seriesTitle;
    private Integer chapterNumber;
    private String title;
    private String publishStatus;
}