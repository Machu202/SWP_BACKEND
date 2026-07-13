package com.mangastudio.backend.service;

import com.mangastudio.backend.dto.response.CanvasInitResponse;
import com.mangastudio.backend.entity.Hitbox;
import com.mangastudio.backend.entity.Task;

import java.util.List;

public interface WorkspaceService {
    Hitbox createHitbox(Long pageId, Long creatorId, Double x, Double y, Double width, Double height);
    List<Hitbox> getHitboxesByPage(Long pageId);
    void deleteHitbox(Long hitboxId, Long currentUserId);
    Task assignTaskToHitbox(Long hitboxId, Long mangakaId, Task taskRequest);
    CanvasInitResponse getCanvasInitData(Long pageId);
}
