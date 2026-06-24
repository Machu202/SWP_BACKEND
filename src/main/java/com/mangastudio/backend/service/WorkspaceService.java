package com.mangastudio.backend.service;

import com.mangastudio.backend.entity.Hitbox;
import com.mangastudio.backend.entity.Task;

import java.util.List;

public interface WorkspaceService {
    
    // Core actions for Backend 2 to implement later
    Hitbox createHitbox(Long pageId, Long creatorId, Double x, Double y, Double width, Double height);
    
    List<Hitbox> getHitboxesByPage(Long pageId);
    
    void deleteHitbox(Long hitboxId);
    
    Task assignTaskToHitbox(Long hitboxId, Long mangakaId, Task taskRequest);
}