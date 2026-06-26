package com.mangastudio.backend.service.impl;

import com.mangastudio.backend.entity.Hitbox;
import com.mangastudio.backend.entity.HitboxComment;
import com.mangastudio.backend.entity.User;
import com.mangastudio.backend.repository.HitboxCommentRepository;
import com.mangastudio.backend.repository.HitboxRepository;
import com.mangastudio.backend.repository.UserRepository;
import com.mangastudio.backend.service.HitboxCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HitboxCommentServiceImpl implements HitboxCommentService {

    private final HitboxCommentRepository hitboxCommentRepository;
    private final HitboxRepository hitboxRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public HitboxComment addCommentToHitbox(Long hitboxId, Long userId, String content) {
        Hitbox hitbox = hitboxRepository.findById(hitboxId)
                .orElseThrow(() -> new RuntimeException("Error: Hitbox not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Error: User not found"));

        HitboxComment comment = HitboxComment.builder()
                .hitbox(hitbox)
                .user(user)
                .content(content)
                .build();

        return hitboxCommentRepository.save(comment);
    }

    @Override
    public List<HitboxComment> getCommentsByHitbox(Long hitboxId) {
        return hitboxCommentRepository.findByHitboxIdOrderByCreatedAtAsc(hitboxId);
    }
}