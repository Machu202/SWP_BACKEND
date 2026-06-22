package com.mangastudio.backend.dto;

public record CanvasProportionResponse(
    Long pageId,
    String imageUrl,
    Double originalWidth,
    Double originalHeight,
    int clientRenderWidth,
    int clientRenderHeight,
    double scaleFactor
) {}