package com.mangastudio.backend.service;

import com.mangastudio.backend.entity.TantouFeedback;
import java.util.List;

public interface TantouFeedbackService {
    TantouFeedback createFeedback(Long pageId, Long editorId, Double x, Double y, Double w, Double h, String content);
    List<TantouFeedback> getFeedbacksByPage(Long pageId);
    TantouFeedback resolveFeedback(Long feedbackId, Long userId);
}