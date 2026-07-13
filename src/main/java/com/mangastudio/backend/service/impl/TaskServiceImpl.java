package com.mangastudio.backend.service.impl;

import com.mangastudio.backend.entity.Task;
import com.mangastudio.backend.entity.User;
import com.mangastudio.backend.repository.TaskRepository;
import com.mangastudio.backend.repository.UserRepository;
import com.mangastudio.backend.service.TaskService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.mangastudio.backend.repository.MangaSeriesRepository;

import java.util.List;

@Service
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final MangaSeriesRepository mangaSeriesRepository;

    public TaskServiceImpl(TaskRepository taskRepository, UserRepository userRepository,
                           MangaSeriesRepository mangaSeriesRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.mangaSeriesRepository = mangaSeriesRepository;
    }

    @Override
    public List<Task> getTasksByMangaka(Long mangakaId) {
        return taskRepository.findByMangakaId(mangakaId);
    }

    @Override
    public List<Task> getTasksByAssistant(Long assistantId) {
        return taskRepository.findByAssistantId(assistantId);
    }

    @Override
    public List<Task> getTasksByTantou(Long tantouId) {
        return taskRepository.findByAssignedTantouId(tantouId);
    }

    @Override
    @Transactional
    public Task assignAssistantToTask(Long taskId, Long mangakaId, Long assistantId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Error: Task not found"));

        if (task.getMangaka() == null || !task.getMangaka().getId().equals(mangakaId)) {
            throw new AccessDeniedException("Only the Mangaka who created this task can assign an assistant.");
        }

        User assistant = userRepository.findById(assistantId)
                .orElseThrow(() -> new RuntimeException("Error: Assistant not found"));
        String assistantRole = assistant.getRole() != null ? assistant.getRole().getRoleName() : "";
        if (!"ASSISTANT".equalsIgnoreCase(assistantRole)) {
            throw new RuntimeException("Error: Selected user is not an Assistant.");
        }

        task.setAssistant(assistant);
        return taskRepository.save(task);
    }

    /**
     * Generic Kanban movement is intentionally narrow. Only the owning Mangaka
     * may advance TODO -> DOING -> REVIEWING. Review decisions use reviewTask()
     * so REVIEWING cannot be dragged backward or directly into APPROVED.
     */
    @Override
    @Transactional
    public Task updateTaskStatus(Long taskId, Long userId, String newStatusStr) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Error: Task not found"));
        User actor = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Error: User not found"));

        String actorRole = actor.getRole() != null ? actor.getRole().getRoleName() : "";
        if (!"MANGAKA".equalsIgnoreCase(actorRole)) {
            throw new AccessDeniedException("Kanban status changes are restricted to the owning Mangaka.");
        }
        if (task.getMangaka() == null || !task.getMangaka().getId().equals(userId)) {
            throw new AccessDeniedException("You do not own this task.");
        }

        String currentStatus = normalizeStatus(task.getStatus());
        String newStatus = normalizeStatus(newStatusStr);
        boolean valid = ("TODO".equals(currentStatus) && "DOING".equals(newStatus))
                || ("DOING".equals(currentStatus) && "REVIEWING".equals(newStatus));

        if (!valid) {
            throw new RuntimeException("Error: Invalid Kanban transition from " + currentStatus + " to " + newStatus
                    + ". Allowed flow is TODO -> DOING -> REVIEWING; approval happens in Mangaka Review.");
        }

        task.setStatus(newStatus);
        return taskRepository.save(task);
    }

    @Override
    @Transactional
    public Task startTask(Long taskId, Long assistantId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Error: Task not found"));
        if (task.getAssistant() == null || !task.getAssistant().getId().equals(assistantId)) {
            throw new AccessDeniedException("You are not assigned to this task.");
        }

        String status = normalizeStatus(task.getStatus());
        if ("TODO".equals(status)) {
            task.setStatus("DOING");
            return taskRepository.save(task);
        }
        // Idempotent download/start action for a task already in progress or later.
        if ("DOING".equals(status) || "REVIEWING".equals(status) || "APPROVED".equals(status)) {
            return task;
        }
        throw new RuntimeException("Error: Task cannot be started from status " + status);
    }

    @Override
    @Transactional
    public Task reviewTask(Long taskId, Long mangakaId, boolean approved) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Error: Task not found"));
        if (task.getMangaka() == null || !task.getMangaka().getId().equals(mangakaId)) {
            throw new AccessDeniedException("Only the owning Mangaka can review this task.");
        }

        String currentStatus = normalizeStatus(task.getStatus());
        if (!"REVIEWING".equals(currentStatus)) {
            throw new RuntimeException("Error: Only REVIEWING tasks can receive a Mangaka decision.");
        }

        task.setStatus(approved ? "APPROVED" : "DOING");
        return taskRepository.save(task);
    }

    @Override
    @Transactional
    public Task submitTaskWork(Long taskId, Long assistantId, String imageUrl) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Error: Task not found"));

        if (task.getAssistant() == null || !task.getAssistant().getId().equals(assistantId)) {
            throw new AccessDeniedException("You are not assigned to this task.");
        }
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new RuntimeException("Error: Submitted image URL is required.");
        }
        if ("APPROVED".equals(normalizeStatus(task.getStatus()))) {
            throw new RuntimeException("Error: Approved tasks cannot be resubmitted.");
        }

        task.setSubmittedImageUrl(imageUrl);
        task.setStatus("REVIEWING");
        return taskRepository.save(task);
    }

    @Override
    public List<Task> getTasksBySeries(Long seriesId) {
        mangaSeriesRepository.findById(seriesId)
                .orElseThrow(() -> new RuntimeException("Error: Manga Series not found with ID: " + seriesId));
        return taskRepository.findAllTasksBySeriesId(seriesId);
    }

    @Override
    public long getSeriesTaskNumber(Task task) {
        if (task == null || task.getId() == null || task.getHitbox() == null || task.getHitbox().getPage() == null
                || task.getHitbox().getPage().getChapter() == null
                || task.getHitbox().getPage().getChapter().getMangaSeries() == null) {
            return task != null && task.getId() != null ? task.getId() : 0L;
        }
        Long seriesId = task.getHitbox().getPage().getChapter().getMangaSeries().getId();
        return taskRepository.countSeriesTasksUpToId(seriesId, task.getId());
    }

    private String normalizeStatus(String status) {
        String value = status == null ? "TODO" : status.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        return switch (value) {
            case "TO_DO", "OPEN", "NEW", "PENDING" -> "TODO";
            case "IN_PROGRESS", "PROGRESS", "WORKING", "REVISION", "CHANGES_REQUESTED", "NEEDS_REVISION" -> "DOING";
            case "REVIEW", "IN_REVIEW", "PENDING_REVIEW", "WAITING_REVIEW", "SUBMITTED", "SUBMITTED_FOR_REVIEW", "AWAITING_REVIEW", "DONE" -> "REVIEWING";
            case "COMPLETE", "COMPLETED", "ACCEPTED", "VERIFIED" -> "APPROVED";
            default -> value.isBlank() ? "TODO" : value;
        };
    }
}
