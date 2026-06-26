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
    
    // Kích thước gốc của ảnh, rất quan trọng để FE tính toán tỉ lệ (Proportions)
    private Double originalWidth;
    private Double originalHeight;
    
    // Danh sách toàn bộ các vùng chọn đã vẽ trên trang này
    private List<Hitbox> hitboxes;
}