package com.mangastudio.backend.service.impl;

import com.mangastudio.backend.entity.Page;
import com.mangastudio.backend.entity.TantouFeedback;
import com.mangastudio.backend.entity.Task;
import com.mangastudio.backend.entity.User;
import com.mangastudio.backend.repository.PageRepository;
import com.mangastudio.backend.repository.TantouFeedbackRepository;
import com.mangastudio.backend.repository.UserRepository;
import com.mangastudio.backend.service.NotificationService;
import com.mangastudio.backend.service.TantouFeedbackService;
import com.mangastudio.backend.service.TaskService;
import com.mangastudio.backend.service.WorkspaceService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TantouFeedbackServiceImpl implements TantouFeedbackService {

    private final TantouFeedbackRepository tantouFeedbackRepository;
    private final PageRepository pageRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final WorkspaceService workspaceService;
    private final TaskService taskService;

    public TantouFeedbackServiceImpl(
            TantouFeedbackRepository tantouFeedbackRepository,
            PageRepository pageRepository,
            UserRepository userRepository,
            NotificationService notificationService,
            WorkspaceService workspaceService,
            TaskService taskService) {
        this.tantouFeedbackRepository = tantouFeedbackRepository;
        this.pageRepository = pageRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.workspaceService = workspaceService;
        this.taskService = taskService;
    }

    @Override
    @Transactional
    public TantouFeedback createFeedback(Long pageId, Long editorId, Double x, Double y, Double w, Double h, String content) {
        Page page = pageRepository.findById(pageId)
                .orElseThrow(() -> new RuntimeException("Error: Page not found"));
        User editor = userRepository.findById(editorId)
                .orElseThrow(() -> new RuntimeException("Error: Editor not found"));

        if (!"Tantou Editor".equalsIgnoreCase(editor.getRole().getRoleName())) {
            throw new AccessDeniedException("Only Tantou Editor can create feedback.");
        }

        User assignedTantou = page.getChapter().getMangaSeries().getTantou();
        if (assignedTantou == null || !assignedTantou.getId().equals(editorId)) {
            throw new AccessDeniedException("Only the Tantou Editor assigned to this series can create feedback on this page.");
        }

        TantouFeedback feedback = TantouFeedback.builder()
                .page(page)
                .editor(editor)
                .xCoord(x)
                .yCoord(y)
                .width(w)
                .height(h)
                .content(content)
                .isResolved(false)
                .build();

        TantouFeedback savedFeedback = tantouFeedbackRepository.save(feedback);
        User mangaka = page.getChapter().getMangaSeries().getMangaka();
        if (mangaka != null && mangaka.getId() != null) {
            notificationService.createNotification(
                    mangaka.getId(),
                    "Tantou \"" + displayName(editor) + "\" has sent you a feedback. Go check it out!",
                    mangakaFeedbackUrl(page, savedFeedback.getId()));
        }
        return savedFeedback;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TantouFeedback> getFeedbacksByPage(Long pageId, Long currentUserId) {
        Page page = pageRepository.findById(pageId)
                .orElseThrow(() -> new RuntimeException("Error: Page not found"));
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Error: User not found"));

        User mangaka = page.getChapter().getMangaSeries().getMangaka();
        User tantou = page.getChapter().getMangaSeries().getTantou();
        String roleName = currentUser.getRole() != null ? currentUser.getRole().getRoleName() : "";
        boolean allowed = "Admin".equalsIgnoreCase(roleName)
                || (mangaka != null && mangaka.getId().equals(currentUserId))
                || (tantou != null && tantou.getId().equals(currentUserId));
        if (!allowed) {
            throw new AccessDeniedException("You do not have permission to view Tantou feedback for this page.");
        }
        return tantouFeedbackRepository.findByPageId(pageId);
    }

    @Override
    @Transactional
    public TantouFeedback addMangakaComment(Long feedbackId, Long userId, String content) {
        TantouFeedback parent = tantouFeedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new RuntimeException("Error: Feedback not found"));
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Error: User not found"));
        User mangaka = parent.getPage().getChapter().getMangaSeries().getMangaka();
        String roleName = currentUser.getRole() != null ? currentUser.getRole().getRoleName() : "";
        boolean allowed = "Admin".equalsIgnoreCase(roleName)
                || (mangaka != null && mangaka.getId().equals(userId));
        if (!allowed) {
            throw new AccessDeniedException("Only the owning Mangaka can comment on Tantou feedback.");
        }
        if (content == null || content.isBlank()) {
            throw new RuntimeException("Error: Comment content is required.");
        }

        TantouFeedback comment = TantouFeedback.builder()
                .page(parent.getPage())
                .editor(currentUser)
                .xCoord(parent.getXCoord())
                .yCoord(parent.getYCoord())
                .width(parent.getWidth())
                .height(parent.getHeight())
                .content("[Mangaka Comment on Feedback #" + parent.getId() + "] " + content.trim())
                .isResolved(false)
                .build();
        return tantouFeedbackRepository.save(comment);
    }

    @Override
    @Transactional
    public TantouFeedback resolveFeedback(Long feedbackId, Long userId) {
        TantouFeedback feedback = tantouFeedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new RuntimeException("Error: Feedback not found"));

        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Error: User not found"));
        User mangaka = feedback.getPage().getChapter().getMangaSeries().getMangaka();
        String roleName = currentUser.getRole() != null ? currentUser.getRole().getRoleName() : "";
        boolean owningMangaka = mangaka != null && mangaka.getId().equals(userId);
        if (!"Admin".equalsIgnoreCase(roleName) && !owningMangaka) {
            throw new AccessDeniedException("Only the owning Mangaka can resolve this Tantou feedback.");
        }

        // Repeated clicks or retries must not create duplicate notifications.
        if (Boolean.TRUE.equals(feedback.getIsResolved())) return feedback;

        feedback.setIsResolved(true);
        TantouFeedback savedFeedback = tantouFeedbackRepository.save(feedback);
        User feedbackEditor = feedback.getEditor();
        boolean authoredByTantou = feedbackEditor != null
                && feedbackEditor.getRole() != null
                && "Tantou Editor".equalsIgnoreCase(feedbackEditor.getRole().getRoleName());
        if (owningMangaka && authoredByTantou && feedbackEditor.getId() != null) {
            Page page = feedback.getPage();
            String seriesTitle = page.getChapter().getMangaSeries().getTitle();
            if (seriesTitle == null || seriesTitle.isBlank()) seriesTitle = "Manga Series";
            notificationService.createNotification(
                    feedbackEditor.getId(),
                    "\"" + seriesTitle.trim() + "\" Mangaka has reviewed your feedback!",
                    tantouFeedbackCanvasUrl(page, savedFeedback.getId()));
        }
        return savedFeedback;
    }

    @Override
    @Transactional
    public Task createAssistantTask(Long feedbackId, Long mangakaId, Long assistantId) {
        TantouFeedback feedback = tantouFeedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new RuntimeException("Error: Feedback not found"));
        User currentUser = userRepository.findById(mangakaId)
                .orElseThrow(() -> new RuntimeException("Error: User not found"));

        Page page = feedback.getPage();
        User owningMangaka = page.getChapter().getMangaSeries().getMangaka();
        String roleName = currentUser.getRole() != null ? currentUser.getRole().getRoleName() : "";
        if (!"Mangaka".equalsIgnoreCase(roleName)
                || owningMangaka == null
                || !owningMangaka.getId().equals(mangakaId)) {
            throw new AccessDeniedException("Only the owning Mangaka can create an Assistant task from this Tantou feedback.");
        }

        User feedbackEditor = feedback.getEditor();
        boolean authoredByTantou = feedbackEditor != null
                && feedbackEditor.getRole() != null
                && "Tantou Editor".equalsIgnoreCase(feedbackEditor.getRole().getRoleName());
        if (!authoredByTantou) {
            throw new AccessDeniedException("Only original Tantou Editor feedback can be converted to an Assistant task.");
        }
        if (feedback.getContent() == null || feedback.getContent().isBlank()) {
            throw new RuntimeException("Error: Tantou feedback description is required.");
        }

        var hitbox = workspaceService.createHitbox(
                page.getId(),
                mangakaId,
                feedback.getXCoord(),
                feedback.getYCoord(),
                feedback.getWidth(),
                feedback.getHeight());
        Task taskRequest = new Task();
        taskRequest.setDescription("BY TANTOU EDITOR\n" + feedback.getContent().trim());
        Task createdTask = workspaceService.assignTaskToHitbox(hitbox.getId(), mangakaId, taskRequest);
        return taskService.assignAssistantToTask(createdTask.getId(), mangakaId, assistantId);
    }

    private String displayName(User user) {
        if (user.getFullName() != null && !user.getFullName().isBlank()) return user.getFullName().trim();
        if (user.getUsername() != null && !user.getUsername().isBlank()) return user.getUsername().trim();
        return "Tantou Editor";
    }

    private String mangakaFeedbackUrl(Page page, Long feedbackId) {
        return "/assistant-review?tab=tantou"
                + "&seriesId=" + page.getChapter().getMangaSeries().getId()
                + "&chapterId=" + page.getChapter().getId()
                + "&pageId=" + page.getId()
                + (feedbackId == null ? "" : "&feedbackId=" + feedbackId);
    }

    private String tantouFeedbackCanvasUrl(Page page, Long feedbackId) {
        return "/canvas-workspace?seriesId=" + page.getChapter().getMangaSeries().getId()
                + "&chapterId=" + page.getChapter().getId()
                + "&pageId=" + page.getId()
                + (feedbackId == null ? "" : "&feedbackId=" + feedbackId);
    }
}
