package com.mangastudio.backend.service;

import com.mangastudio.backend.entity.HitboxComment;
import java.util.List;

public interface HitboxCommentService {
    HitboxComment addCommentToHitbox(Long hitboxId, Long userId, String content);
    List<HitboxComment> getCommentsByHitbox(Long hitboxId);
}