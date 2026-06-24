package com.mangastudio.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class PublishingScheduleRequest {

    @NotNull(message = "Series ID is required")
    private Long seriesId;

    @NotNull(message = "Publish date is required")
    private LocalDateTime publishDate;

    @NotBlank(message = "Frequency is required (e.g., Weekly, Monthly)")
    private String frequency;
}