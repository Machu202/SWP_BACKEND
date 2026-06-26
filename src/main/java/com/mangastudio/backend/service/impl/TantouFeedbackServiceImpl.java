package com.mangastudio.backend.service.impl;

import com.mangastudio.backend.entity.Page;
import com.mangastudio.backend.entity.TantouFeedback;
import com.mangastudio.backend.entity.User;
import com.mangastudio.backend.repository.PageRepository;
import com.mangastudio.backend.repository.TantouFeedbackRepository;
import com.mangastudio.backend.repository.UserRepository;
import com.mangastudio.backend.service.TantouFeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TantouFeedbackServiceImpl implements TantouFeedbackService {

    private final TantouFeedbackRepository tantouFeedbackRepository;
    private final PageRepository pageRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public TantouFeedback createFeedback(Long pageId, Long editorId, Double x, Double y, Double w, Double h, String content) {
        Page page = pageRepository.findById(pageId)
                .orElseThrow(() -> new RuntimeException("Error: Page not found"));
        User editor = userRepository.findById(editorId)
                .orElseThrow(() -> new RuntimeException("Error: Editor not found"));

        if (!"Tantou Editor".equalsIgnoreCase(editor.getRole().getRoleName())) {
            throw new RuntimeException("Error: Only Tantou Editor can create feedback.");
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
    public List<TantouFeedback> getFeedbacksByPage(Long pageId) {
        return tantouFeedbackRepository.findByPageId(pageId);
    }

    @Override
    @Transactional
    public TantouFeedback resolveFeedback(Long feedbackId, Long userId) {
        TantouFeedback feedback = tantouFeedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new RuntimeException("Error: Feedback not found"));

        feedback.setIsResolved(true);
        return tantouFeedbackRepository.save(feedback);
    }
}