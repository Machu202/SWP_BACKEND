package com.mangastudio.backend.service.impl;

import com.mangastudio.backend.entity.Page;
import com.mangastudio.backend.entity.TantouFeedback;
import com.mangastudio.backend.entity.User;
import com.mangastudio.backend.repository.PageRepository;
import com.mangastudio.backend.repository.TantouFeedbackRepository;
import com.mangastudio.backend.repository.UserRepository;
import com.mangastudio.backend.service.TantouFeedbackService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TantouFeedbackServiceImpl implements TantouFeedbackService {

    private final TantouFeedbackRepository tantouFeedbackRepository;
    private final PageRepository pageRepository;
    private final UserRepository userRepository;

    public TantouFeedbackServiceImpl(
            TantouFeedbackRepository tantouFeedbackRepository,
            PageRepository pageRepository,
            UserRepository userRepository) {
        this.tantouFeedbackRepository = tantouFeedbackRepository;
        this.pageRepository = pageRepository;
        this.userRepository = userRepository;
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

        return tantouFeedbackRepository.save(feedback);
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
        if (!"Admin".equalsIgnoreCase(roleName)
                && (mangaka == null || !mangaka.getId().equals(userId))) {
            throw new AccessDeniedException("Only the owning Mangaka can resolve this Tantou feedback.");
        }

        feedback.setIsResolved(true);
        return tantouFeedbackRepository.save(feedback);
    }
}