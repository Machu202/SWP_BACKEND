package com.mangastudio.backend.service.impl;

import com.mangastudio.backend.dto.response.CanvasInitResponse;
import com.mangastudio.backend.entity.Hitbox;
import com.mangastudio.backend.entity.Page;
import com.mangastudio.backend.entity.Task;
import com.mangastudio.backend.entity.User;
import com.mangastudio.backend.repository.HitboxCommentRepository;
import com.mangastudio.backend.repository.HitboxRepository;
import com.mangastudio.backend.repository.PageRepository;
import com.mangastudio.backend.repository.TaskRepository;
import com.mangastudio.backend.repository.UserRepository;
import com.mangastudio.backend.service.WorkspaceService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WorkspaceServiceImpl implements WorkspaceService {

    private final HitboxRepository hitboxRepository;
    private final PageRepository pageRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final HitboxCommentRepository hitboxCommentRepository;

    public WorkspaceServiceImpl(
            HitboxRepository hitboxRepository,
            PageRepository pageRepository,
            UserRepository userRepository,
            TaskRepository taskRepository,
            HitboxCommentRepository hitboxCommentRepository) {
        this.hitboxRepository = hitboxRepository;
        this.pageRepository = pageRepository;
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.hitboxCommentRepository = hitboxCommentRepository;
    }

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

        return hitboxRepository.findByPageIdAndPageVersionIsNull(pageId);
    }

    @Override
    @Transactional
    public void deleteHitbox(Long hitboxId, Long currentUserId) {
        Hitbox hitbox = hitboxRepository.findById(hitboxId)
                .orElseThrow(() -> new RuntimeException("Error: Hitbox not found with ID: " + hitboxId));

        Long ownerMangakaId = hitbox.getPage()
                .getChapter()
                .getMangaSeries()
                .getMangaka()
                .getId();
        if (!ownerMangakaId.equals(currentUserId)) {
            throw new AccessDeniedException("Only the Mangaka who owns this series can delete the hitbox.");
        }

        Task connectedTask = taskRepository.findByHitboxId(hitboxId);
        if (connectedTask != null) {
            throw new RuntimeException("This hitbox already has a task. Remove or cancel the task before deleting the hitbox.");
        }

        // Comments are child records but the legacy schema does not declare a
        // cascading relationship on Hitbox, so remove them explicitly first.
        hitboxCommentRepository.deleteByHitboxId(hitboxId);
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
                .referenceImageUrl(hitbox.getPage() != null ? hitbox.getPage().getImageUrl() : null)
                .status("TODO")
                .description(taskRequest.getDescription())
                .build();

        return taskRepository.save(newTask);
    }

    @Override
    public CanvasInitResponse getCanvasInitData(Long pageId) {
        Page page = pageRepository.findById(pageId)
                .orElseThrow(() -> new RuntimeException("Error: Page not found with ID: " + pageId));

        List<Hitbox> hitboxes = hitboxRepository.findByPageIdAndPageVersionIsNull(pageId);

        return CanvasInitResponse.builder()
                .pageId(page.getId())
                .imageUrl(page.getImageUrl())
                .originalWidth(page.getWidth())
                .originalHeight(page.getHeight())
                .hitboxes(hitboxes)
                .build();
    }
}
