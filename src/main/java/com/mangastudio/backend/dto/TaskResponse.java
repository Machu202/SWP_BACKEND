package com.mangastudio.backend.dto;

import java.time.LocalDateTime;

public record TaskResponse(
    Long taskId,
    Long pageId,
    Long mangakaId,
    Long assistantId,
    String status,
    String taskDesc,
    Double xNorm,
    Double yNorm,
    Double widthNorm,
    Double heightNorm,
    String submittedImageUrl,
    LocalDateTime deadline
) {}