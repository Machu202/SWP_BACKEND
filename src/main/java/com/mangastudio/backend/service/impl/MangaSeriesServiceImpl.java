package com.mangastudio.backend.service.impl;

import com.mangastudio.backend.dto.request.MangaSeriesCreateRequest;
import com.mangastudio.backend.dto.response.MangaSeriesResponse;
import com.mangastudio.backend.entity.MangaSeries;
import com.mangastudio.backend.entity.User;
import com.mangastudio.backend.repository.MangaSeriesRepository;
import com.mangastudio.backend.repository.UserRepository;
import com.mangastudio.backend.service.MangaSeriesService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MangaSeriesServiceImpl implements MangaSeriesService {

    private final MangaSeriesRepository mangaSeriesRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public MangaSeriesResponse createSeries(Long mangakaId, MangaSeriesCreateRequest request) {
        User mangaka = userRepository.findById(mangakaId)
                .orElseThrow(() -> new RuntimeException("Error: Mangaka not found with ID: " + mangakaId));

        // Note: New series start with "Draft" status, Tantou is null until assigned
        MangaSeries newSeries = MangaSeries.builder()
                .mangaka(mangaka)
                .title(request.getTitle())
                .genre(request.getGenre())
                .summary(request.getSummary())
                .status("Draft")
                .createdAt(LocalDateTime.now())
                .build();

        MangaSeries savedSeries = mangaSeriesRepository.save(newSeries);
        return mapToResponse(savedSeries);
    }

    @Override
    public MangaSeriesResponse getSeriesById(Long seriesId) {
        MangaSeries series = mangaSeriesRepository.findById(seriesId)
                .orElseThrow(() -> new RuntimeException("Error: Manga Series not found with ID: " + seriesId));
        return mapToResponse(series);
    }

    @Override
    public List<MangaSeriesResponse> getAllSeriesByMangaka(Long mangakaId) {
        // You will need to add findByMangakaId in MangaSeriesRepository for this to work natively,
        // or filter it here. Assuming we add it to the repository later.
        User mangaka = userRepository.findById(mangakaId)
                .orElseThrow(() -> new RuntimeException("Error: Mangaka not found"));
                
        // Fallback using streams if repository method isn't created yet
        return mangaSeriesRepository.findAll().stream()
                .filter(series -> series.getMangaka().getId().equals(mangakaId))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MangaSeriesResponse updateSeriesStatus(Long seriesId, String newStatus) {
        MangaSeries series = mangaSeriesRepository.findById(seriesId)
                .orElseThrow(() -> new RuntimeException("Error: Manga Series not found"));
        
        // State Machine Logic: You can add validation here to ensure valid transitions 
        // (e.g., Draft -> Reviewing -> Ongoing)
        series.setStatus(newStatus);
        
        MangaSeries updatedSeries = mangaSeriesRepository.save(series);
        return mapToResponse(updatedSeries);
    }

    // Helper method to convert Entity to Response DTO
    private MangaSeriesResponse mapToResponse(MangaSeries series) {
        String tantouName = (series.getTantou() != null) ? series.getTantou().getFullName() : "Unassigned";
        
        return MangaSeriesResponse.builder()
                .id(series.getId())
                .title(series.getTitle())
                .genre(series.getGenre())
                .summary(series.getSummary())
                .status(series.getStatus())
                .mangakaName(series.getMangaka().getFullName())
                .tantouName(tantouName)
                .createdAt(series.getCreatedAt())
                .build();
    }
}