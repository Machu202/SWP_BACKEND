package com.mangastudio.backend.service;

import com.mangastudio.backend.dto.request.ChapterCreateRequest;
import com.mangastudio.backend.dto.response.ChapterResponse;
import java.util.List;

public interface ChapterService {
    ChapterResponse createChapter(Long currentUserId, ChapterCreateRequest request);
    ChapterResponse getChapterById(Long chapterId);
    List<ChapterResponse> getAllChaptersBySeries(Long seriesId);
    
    // FE-30: Cập nhật trạng thái
    ChapterResponse updateChapterStatus(Long chapterId, Long currentUserId, String newStatus);
    
    void deleteChapter(Long chapterId, Long currentUserId);
}