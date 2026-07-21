package com.mangastudio.backend.dto.response;

import com.mangastudio.backend.entity.Hitbox;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class CanvasInitResponse {
    private Long pageId;
    private String imageUrl;
    
    // Original image dimensions used by the frontend to preserve proportions.
    private Double originalWidth;
    private Double originalHeight;
    
    // Every hitbox drawn on this page.
    private List<Hitbox> hitboxes;
}
