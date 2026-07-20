package com.mangastudio.backend.service;

import com.mangastudio.backend.dto.request.ChapterCreateRequest;
import com.mangastudio.backend.dto.response.ChapterResponse;
import com.mangastudio.backend.entity.PublishingSchedule;
import java.time.LocalDateTime;
import java.util.List;

public interface ChapterService {
    ChapterResponse createChapter(Long currentUserId, ChapterCreateRequest request);
    ChapterResponse getChapterById(Long chapterId);
    List<ChapterResponse> getAllChaptersBySeries(Long seriesId);
    List<ChapterResponse> getTantouReviewQueue(Long currentUserId);
    ChapterResponse updateChapterStatus(Long chapterId, Long currentUserId, String newStatus);
    PublishingSchedule schedulePublication(Long chapterId, Long currentUserId, LocalDateTime publishAt);
    void publishScheduledChapter(Long chapterId);
    void deleteChapter(Long chapterId, Long currentUserId);
}
