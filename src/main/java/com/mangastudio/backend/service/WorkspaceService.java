package com.mangastudio.backend.service;

import com.mangastudio.backend.dto.CanvasProportionResponse;
import com.mangastudio.backend.dto.CreateTaskRequest;
import com.mangastudio.backend.dto.TaskResponse;
import com.mangastudio.backend.entity.Page;
import com.mangastudio.backend.entity.Task;
import com.mangastudio.backend.repository.PageRepository;
import com.mangastudio.backend.repository.TaskRepository;
import com.mangastudio.backend.util.SpatialEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final PageRepository pageRepo;
    private final TaskRepository taskRepo;

    public CanvasProportionResponse getCanvasProportions(Long pageId, int clientContainerWidth) {
        Page page = pageRepo.findById(pageId)
                .orElseThrow(() -> new RuntimeException("Page not found with ID: " + pageId));

        var prop = SpatialEngine.calculateProportions(page.getWidth(), page.getHeight(), clientContainerWidth);

        return new CanvasProportionResponse(
                page.getPageId(), page.getImageUrl(),
                page.getWidth(), page.getHeight(),
                prop.renderWidth(), prop.renderHeight(), prop.scaleFactor()
        );
    }

    @Transactional
    public TaskResponse assignTaskToAssistant(Long pageId, Long mangakaId, CreateTaskRequest req) {
        Page page = pageRepo.findById(pageId)
                .orElseThrow(() -> new RuntimeException("Page does not exist"));

        Task task = Task.builder()
                .pageId(page.getPageId())
                .mangakaId(mangakaId)
                .assistantId(req.assigneeId())
                .status(Task.TaskStatus.TODO)
                .taskDesc(req.taskDesc())
                .xNorm(req.xNorm())
                .yNorm(req.yNorm())
                .widthNorm(req.widthNorm())
                .heightNorm(req.heightNorm())
                .deadline(req.deadline())
                .build();

        task = taskRepo.save(task);
        return mapToResponse(task);
    }

    private TaskResponse mapToResponse(Task t) {
        return new TaskResponse(
                t.getTaskId(), t.getPageId(), t.getMangakaId(), t.getAssistantId(),
                t.getStatus().name(), t.getTaskDesc(), t.getXNorm(), t.getYNorm(),
                t.getWidthNorm(), t.getHeightNorm(), t.getSubmittedImageUrl(), t.getDeadline()
        );
    }
}