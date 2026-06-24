package com.mangastudio.backend.service;

import com.mangastudio.backend.dto.request.MangaSeriesCreateRequest;
import com.mangastudio.backend.dto.response.MangaSeriesResponse;
import java.util.List;

public interface MangaSeriesService {
    MangaSeriesResponse createSeries(Long mangakaId, MangaSeriesCreateRequest request);
    MangaSeriesResponse getSeriesById(Long seriesId);
    List<MangaSeriesResponse> getAllSeriesByMangaka(Long mangakaId);
    MangaSeriesResponse updateSeriesStatus(Long seriesId, String newStatus);
}