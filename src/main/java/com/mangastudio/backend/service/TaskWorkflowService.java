package com.mangastudio.backend.service;

import com.mangastudio.backend.dto.TaskResponse;
import com.mangastudio.backend.entity.Page;
import com.mangastudio.backend.entity.Task;
import com.mangastudio.backend.repository.PageRepository;
import com.mangastudio.backend.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaskWorkflowService {

    private final TaskRepository taskRepo;
    private final PageRepository pageRepo;

    @Transactional
    public TaskResponse submitAssistantWork(Long taskId, Long assistantId, String resultImageUrl) {
        Task task = taskRepo.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (!task.getAssistantId().equals(assistantId)) {
            throw new RuntimeException("Unauthorized: You are not assigned to this task");
        }

        task.setSubmittedImageUrl(resultImageUrl);
        task.setStatus(Task.TaskStatus.REVIEWING);
        return mapToResponse(taskRepo.save(task));
    }

    @Transactional
    public TaskResponse reviewTaskByMangaka(Long taskId, Long mangakaId, boolean isApproved) {
        Task task = taskRepo.findById(taskId).orElseThrow();

        if (isApproved) {
            task.setStatus(Task.TaskStatus.APPROVED);
            taskRepo.save(task);

            checkAndTriggerChapterReady(task.getPageId());
        } else {
            task.setStatus(Task.TaskStatus.REJECTED);
            taskRepo.save(task);
        }
        return mapToResponse(task);
    }

    private void checkAndTriggerChapterReady(Long pageId) {
        long pendingInPage = taskRepo.countByPageIdAndStatusNot(pageId, Task.TaskStatus.APPROVED);
        if (pendingInPage > 0) return;

        Page currentPage = pageRepo.findById(pageId).orElseThrow();
        Long chapterId = currentPage.getChapterId();

        var allPages = pageRepo.findByChapterIdOrderByPageNumberAsc(chapterId);
        boolean isChapterFullyApproved = true;

        for (Page p : allPages) {
            long pending = taskRepo.countByPageIdAndStatusNot(p.getPageId(), Task.TaskStatus.APPROVED);
            if (pending > 0) {
                isChapterFullyApproved = false;
                break;
            }
        }

        if (isChapterFullyApproved) {
            System.out.println(">>> [BE2 EVENT] CHAPTER " + chapterId + " IS 100% DRAWN! AUTOMATICALLY TRIGGERING READY_FOR_EDITOR STATE!");
        }
    }

    private TaskResponse mapToResponse(Task t) {
        return new TaskResponse(
                t.getTaskId(), t.getPageId(), t.getMangakaId(), t.getAssistantId(),
                t.getStatus().name(), t.getTaskDesc(), t.getXNorm(), t.getYNorm(),
                t.getWidthNorm(), t.getHeightNorm(), t.getSubmittedImageUrl(), t.getDeadline()
        );
    }
}