package com.mangastudio.backend.service;

import com.mangastudio.backend.dto.request.MangaSeriesCreateRequest;
import com.mangastudio.backend.dto.request.MangaSeriesUpdateRequest;
import com.mangastudio.backend.dto.response.MangaSeriesResponse;
import com.mangastudio.backend.entity.MangaSeries;
import com.mangastudio.backend.entity.PublishingSchedule;
import org.springframework.data.domain.Page;
import java.time.LocalDateTime;
import java.util.List;

public interface MangaSeriesService {
    MangaSeriesResponse createSeries(Long mangakaId, MangaSeriesCreateRequest request);
    MangaSeriesResponse getSeriesById(Long seriesId);
    List<MangaSeriesResponse> getAllSeriesByMangaka(Long mangakaId);
    List<MangaSeriesResponse> getAllSeriesAssignedToTantou(Long tantouId);
    MangaSeriesResponse updateSeriesStatus(Long seriesId, Long currentUserId, String newStatus);
    PublishingSchedule schedulePublication(Long seriesId, Long currentUserId, LocalDateTime publishAt);
    void publishScheduledSeries(Long seriesId);
    MangaSeriesResponse assignTantou(Long seriesId, Long currentUserId, Long tantouId);
    MangaSeriesResponse submitToEditorialBoard(Long seriesId, Long currentUserId);
    
    // Updates manga metadata and deletes a manga series.
    MangaSeriesResponse updateSeriesMetadata(Long seriesId, Long currentUserId, MangaSeriesUpdateRequest request);
    void deleteSeries(Long seriesId, Long currentUserId);
    // [FE-17] Records the Admin's final decision.
    MangaSeriesResponse adminApproveSeries(Long seriesId, boolean isApproved);
    MangaSeries handleAdminDecision(Long seriesId, Boolean isApproved, Long tantouId);

    Page<MangaSeriesResponse> getSeriesByStatus(String status, int page, int size);
}
