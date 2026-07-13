package com.mangastudio.backend.service;

import com.mangastudio.backend.dto.request.MangaSeriesCreateRequest;
import com.mangastudio.backend.dto.request.MangaSeriesUpdateRequest;
import com.mangastudio.backend.dto.response.MangaSeriesResponse;
import com.mangastudio.backend.entity.MangaSeries;
import org.springframework.data.domain.Page;
import java.util.List;

public interface MangaSeriesService {
    MangaSeriesResponse createSeries(Long mangakaId, MangaSeriesCreateRequest request);
    MangaSeriesResponse getSeriesById(Long seriesId);
    List<MangaSeriesResponse> getAllSeriesByMangaka(Long mangakaId);
    MangaSeriesResponse updateSeriesStatus(Long seriesId, Long currentUserId, String newStatus);
    MangaSeriesResponse assignTantou(Long seriesId, Long currentUserId, Long tantouId);
    MangaSeriesResponse submitToEditorialBoard(Long seriesId, Long currentUserId);
    
    // [BỔ SUNG] Cập nhật thông tin truyện và Xóa truyện
    MangaSeriesResponse updateSeriesMetadata(Long seriesId, Long currentUserId, MangaSeriesUpdateRequest request);
    void deleteSeries(Long seriesId, Long currentUserId);
    // [FE-17] Khai báo hàm Admin chốt duyệt cuối cùng
    MangaSeriesResponse adminApproveSeries(Long seriesId, boolean isApproved);
    MangaSeries handleAdminDecision(Long seriesId, Boolean isApproved, Long tantouId);

    Page<MangaSeriesResponse> getSeriesByStatus(String status, int page, int size);
}