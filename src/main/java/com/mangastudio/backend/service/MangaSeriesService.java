package com.mangastudio.backend.service;

import com.mangastudio.backend.dto.request.MangaSeriesCreateRequest;
import com.mangastudio.backend.dto.request.MangaSeriesUpdateRequest;
import com.mangastudio.backend.dto.response.MangaSeriesResponse;
import java.util.List;

public interface MangaSeriesService {
    MangaSeriesResponse createSeries(Long mangakaId, MangaSeriesCreateRequest request);
    MangaSeriesResponse getSeriesById(Long seriesId);
    List<MangaSeriesResponse> getAllSeriesByMangaka(Long mangakaId);
    MangaSeriesResponse updateSeriesStatus(Long seriesId, String newStatus);
    
    // [BỔ SUNG] Cập nhật thông tin truyện và Xóa truyện
    MangaSeriesResponse updateSeriesMetadata(Long seriesId, Long currentUserId, MangaSeriesUpdateRequest request);
    void deleteSeries(Long seriesId, Long currentUserId);
    // [FE-17] Khai báo hàm Admin chốt duyệt cuối cùng
    MangaSeriesResponse adminApproveSeries(Long seriesId, boolean isApproved);
}