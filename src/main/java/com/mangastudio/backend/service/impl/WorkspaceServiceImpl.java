package com.mangastudio.backend.service.impl;

import com.mangastudio.backend.dto.response.CanvasInitResponse;
import com.mangastudio.backend.entity.Hitbox;
import com.mangastudio.backend.entity.Page;
import com.mangastudio.backend.entity.Task;
import com.mangastudio.backend.entity.User;
import com.mangastudio.backend.repository.HitboxRepository;
import com.mangastudio.backend.repository.PageRepository;
import com.mangastudio.backend.repository.TaskRepository;
import com.mangastudio.backend.repository.UserRepository;
import com.mangastudio.backend.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkspaceServiceImpl implements WorkspaceService {

    private final HitboxRepository hitboxRepository;
    private final PageRepository pageRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;

    @Override
    @Transactional
    public Hitbox createHitbox(Long pageId, Long creatorId, Double x, Double y, Double width, Double height) {
        Page page = pageRepository.findById(pageId)
                .orElseThrow(() -> new RuntimeException("Error: Page not found with ID: " + pageId));
        
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("Error: User not found with ID: " + creatorId));

        Hitbox hitbox = Hitbox.builder()
                .page(page)
                .createdBy(creator)
                .xCoord(x)
                .yCoord(y)
                .width(width)
                .height(height)
                .build();

        return hitboxRepository.save(hitbox);
    }

    @Override
    public List<Hitbox> getHitboxesByPage(Long pageId) {
        pageRepository.findById(pageId)
                .orElseThrow(() -> new RuntimeException("Error: Page not found with ID: " + pageId));
                
        return hitboxRepository.findByPageId(pageId);
    }

    @Override
    @Transactional
    public void deleteHitbox(Long hitboxId) {
        Hitbox hitbox = hitboxRepository.findById(hitboxId)
                .orElseThrow(() -> new RuntimeException("Error: Hitbox not found with ID: " + hitboxId));
        
        hitboxRepository.delete(hitbox);
    }

    @Override
    @Transactional
    public Task assignTaskToHitbox(Long hitboxId, Long mangakaId, Task taskRequest) {
        Hitbox hitbox = hitboxRepository.findById(hitboxId)
                .orElseThrow(() -> new RuntimeException("Error: Hitbox not found with ID: " + hitboxId));
                
        User mangaka = userRepository.findById(mangakaId)
                .orElseThrow(() -> new RuntimeException("Error: Mangaka not found with ID: " + mangakaId));

        Task newTask = Task.builder()
                .hitbox(hitbox)
                .mangaka(mangaka)
                .status("TODO") 
                .description(taskRequest.getDescription())
                .build();

        return taskRepository.save(newTask);
    }
    @Override
    public CanvasInitResponse getCanvasInitData(Long pageId) {
        // 1. Lấy thông tin trang truyện gốc
        Page page = pageRepository.findById(pageId)
                .orElseThrow(() -> new RuntimeException("Error: Page not found with ID: " + pageId));
                
        // 2. Lấy toàn bộ Hitbox thuộc về trang này
        List<Hitbox> hitboxes = hitboxRepository.findByPageId(pageId);

        // 3. Đóng gói tất cả vào một Response duy nhất để trả về cho Frontend
        return CanvasInitResponse.builder()
                .pageId(page.getId())
                .imageUrl(page.getImageUrl())
                .originalWidth(page.getWidth())
                .originalHeight(page.getHeight())
                .hitboxes(hitboxes)
                .build();
    }
}