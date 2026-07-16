package com.mangastudio.backend.service.impl;

import com.mangastudio.backend.entity.Task;
import com.mangastudio.backend.entity.User;
import com.mangastudio.backend.repository.TaskRepository;
import com.mangastudio.backend.repository.UserRepository;
import com.mangastudio.backend.repository.PageRepository;
import com.mangastudio.backend.repository.PageVersionRepository;
import com.mangastudio.backend.repository.HitboxRepository;
import com.mangastudio.backend.entity.Page;
import com.mangastudio.backend.entity.PageVersion;
import java.time.LocalDateTime;
import com.mangastudio.backend.service.TaskService;
import com.mangastudio.backend.service.NotificationService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.mangastudio.backend.repository.MangaSeriesRepository;

import java.util.List;
import java.util.Optional;

@Service
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final MangaSeriesRepository mangaSeriesRepository;
    private final PageRepository pageRepository;
    private final PageVersionRepository pageVersionRepository;
    private final HitboxRepository hitboxRepository;
    private final NotificationService notificationService;

    public TaskServiceImpl(TaskRepository taskRepository, UserRepository userRepository,
                           MangaSeriesRepository mangaSeriesRepository,
                           PageRepository pageRepository,
                           PageVersionRepository pageVersionRepository,
                           HitboxRepository hitboxRepository,
                           NotificationService notificationService) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.mangaSeriesRepository = mangaSeriesRepository;
        this.pageRepository = pageRepository;
        this.pageVersionRepository = pageVersionRepository;
        this.hitboxRepository = hitboxRepository;
        this.notificationService = notificationService;
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

        String currentStatus = normalizeStatus(task.getStatus());
        if ("REVIEWING".equals(currentStatus) || "APPROVED".equals(currentStatus)) {
            throw new RuntimeException("Error: Assistant assignment is locked once a task is REVIEWING or APPROVED.");
        }

        User assistant = userRepository.findById(assistantId)
                .orElseThrow(() -> new RuntimeException("Error: Assistant not found"));
        String assistantRole = assistant.getRole() != null ? assistant.getRole().getRoleName() : "";
        if (!"ASSISTANT".equalsIgnoreCase(assistantRole)) {
            throw new RuntimeException("Error: Selected user is not an Assistant.");
        }

        Long previousAssistantId = task.getAssistant() != null ? task.getAssistant().getId() : null;
        task.setAssistant(assistant);
        Task savedTask = taskRepository.save(task);

        if (!assistant.getId().equals(previousAssistantId)) {
            String seriesTitle = taskSeriesTitle(savedTask);
            notificationService.createNotification(
                    assistant.getId(),
                    "You got a new task from \"" + seriesTitle + "\" mangaka!",
                    "/tasks?tab=assignments");
        }
        return savedTask;
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
        if ("REVIEWING".equals(newStatus)
                && (task.getSubmittedImageUrl() == null || task.getSubmittedImageUrl().isBlank())) {
            throw new RuntimeException("Error: A submitted image is required before this task can move to REVIEWING.");
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
        if ("APPROVED".equals(currentStatus) && approved) {
            // Idempotent repair path for legacy APPROVED tasks created before
            // page promotion/versioning existed. The UI remains read-only, but
            // an explicit backend repair call can synchronize old staging data.
            promoteApprovedSubmissionToPage(task);
            return taskRepository.save(task);
        }
        if (!"REVIEWING".equals(currentStatus)) {
            throw new RuntimeException("Error: Only REVIEWING tasks can receive a Mangaka decision.");
        }

        if (approved) {
            promoteApprovedSubmissionToPage(task);
            task.setStatus("APPROVED");
        } else {
            task.setStatus("DOING");
        }
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

        task.setSubmittedImageUrl(imageUrl.trim());
        task.setStatus("REVIEWING");
        Task savedTask = taskRepository.save(task);

        if (savedTask.getMangaka() != null) {
            String assistantName = displayName(savedTask.getAssistant());
            notificationService.createNotification(
                    savedTask.getMangaka().getId(),
                    "Assistant \"" + assistantName + "\" has sent you his work. Go check it out!",
                    "/assistant-review");
        }
        return savedTask;
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

    /**
     * A Mangaka approval promotes the Assistant's submitted page to the live
     * Canvas image and archives it as the next immutable PageVersion. The
     * original task reference is captured before the page changes so Review
     * and Assignments never replace the source image with submitted work.
     */
    private void promoteApprovedSubmissionToPage(Task task) {
        if (task.getSubmittedImageUrl() == null || task.getSubmittedImageUrl().isBlank()) {
            throw new RuntimeException("Error: Cannot approve a task without a submitted image.");
        }
        if (task.getHitbox() == null || task.getHitbox().getPage() == null) {
            throw new RuntimeException("Error: Task is not linked to a manga page.");
        }

        Page page = task.getHitbox().getPage();
        String currentImage = page.getImageUrl();
        if (task.getReferenceImageUrl() == null || task.getReferenceImageUrl().isBlank()) {
            task.setReferenceImageUrl(currentImage);
        }

        String submittedImage = task.getSubmittedImageUrl().trim();
        if (submittedImage.equals(currentImage)) {
            return; // Idempotent protection: never clone a version for the same image.
        }

        Optional<PageVersion> latestVersion = pageVersionRepository.findTopByPageIdOrderByVersionNumberDesc(page.getId());
        int nextVersionNumber = latestVersion.map(version -> version.getVersionNumber() + 1).orElse(1);

        // Ensure the current live image has an immutable version record. Uploaded
        // pages normally already have Version 1, while legacy rows may not.
        PageVersion previousVersion = pageVersionRepository
                .findTopByPageIdAndImageUrlOrderByVersionNumberDesc(page.getId(), currentImage)
                .orElseGet(() -> {
                    PageVersion snapshot = PageVersion.builder()
                            .page(page)
                            .imageUrl(currentImage)
                            .versionNumber(nextVersionNumber)
                            .createdAt(LocalDateTime.now())
                            .build();
                    return pageVersionRepository.save(snapshot);
                });

        int promotedVersionNumber = Math.max(
                previousVersion.getVersionNumber() + 1,
                pageVersionRepository.findTopByPageIdOrderByVersionNumberDesc(page.getId())
                        .map(version -> version.getVersionNumber() + 1)
                        .orElse(1));

        // Archive every hitbox that belongs to the old live image. Their tasks and
        // coordinates remain available from the historical PageVersion, while the
        // newly approved page starts with no active Mangaka hitboxes.
        List<com.mangastudio.backend.entity.Hitbox> liveHitboxes =
                hitboxRepository.findByPageIdAndPageVersionIsNull(page.getId());
        liveHitboxes.forEach(hitbox -> hitbox.setPageVersion(previousVersion));
        if (!liveHitboxes.isEmpty()) hitboxRepository.saveAll(liveHitboxes);

        page.setImageUrl(submittedImage);
        Page savedPage = pageRepository.save(page);
        pageVersionRepository.save(PageVersion.builder()
                .page(savedPage)
                .imageUrl(submittedImage)
                .versionNumber(promotedVersionNumber)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private String displayName(User user) {
        if (user == null) return "Assistant";
        if (user.getFullName() != null && !user.getFullName().isBlank()) return user.getFullName();
        if (user.getUsername() != null && !user.getUsername().isBlank()) return user.getUsername();
        if (user.getEmail() != null && !user.getEmail().isBlank()) return user.getEmail();
        return "Assistant";
    }

    private String taskSeriesTitle(Task task) {
        if (task != null && task.getHitbox() != null && task.getHitbox().getPage() != null
                && task.getHitbox().getPage().getChapter() != null
                && task.getHitbox().getPage().getChapter().getMangaSeries() != null) {
            String title = task.getHitbox().getPage().getChapter().getMangaSeries().getTitle();
            if (title != null && !title.isBlank()) return title;
        }
        return "your assigned series";
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
