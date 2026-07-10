package com.mangastudio.backend.service.impl;

import com.mangastudio.backend.dto.request.MangaSeriesCreateRequest;
import com.mangastudio.backend.dto.request.MangaSeriesUpdateRequest;
import com.mangastudio.backend.dto.response.MangaSeriesResponse;
import com.mangastudio.backend.entity.MangaSeries;
import com.mangastudio.backend.entity.User;
import com.mangastudio.backend.repository.MangaSeriesRepository;
import com.mangastudio.backend.repository.UserRepository;
import com.mangastudio.backend.service.MangaSeriesService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.mangastudio.backend.repository.BoardVoteRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
@Service
@RequiredArgsConstructor
public class MangaSeriesServiceImpl implements MangaSeriesService {

    private final MangaSeriesRepository mangaSeriesRepository;
    private final UserRepository userRepository;
    private final BoardVoteRepository boardVoteRepository;

    @Override
    @Transactional
    public MangaSeriesResponse createSeries(Long mangakaId, MangaSeriesCreateRequest request) {
        User mangaka = userRepository.findById(mangakaId)
                .orElseThrow(() -> new RuntimeException("Error: Mangaka not found with ID: " + mangakaId));

        MangaSeries newSeries = MangaSeries.builder()
                .mangaka(mangaka)
                .title(request.getTitle())
                .genre(request.getGenre())
                .summary(request.getSummary())
                .description(firstNonBlank(request.getDescription(), request.getSummary()))
                .coverImageUrl(firstNonBlank(request.getCoverImageUrl(), request.getCoverUrl(), request.getImageUrl(), request.getThumbnailUrl(), request.getCoverImage(), request.getPrimaryArtUrl()))
                .status("DRAFT")
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
        userRepository.findById(mangakaId)
                .orElseThrow(() -> new RuntimeException("Error: Mangaka not found"));

        return mangaSeriesRepository.findByMangakaId(mangakaId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MangaSeriesResponse updateSeriesStatus(Long seriesId, String newStatusStr) {
        MangaSeries series = mangaSeriesRepository.findById(seriesId)
                .orElseThrow(() -> new RuntimeException("Error: Manga Series not found"));

        String currentStatus = series.getStatus();
        String newStatus = newStatusStr.toUpperCase();
        String normalizedCurrentStatus = (currentStatus != null) ? currentStatus.toUpperCase() : "DRAFT";

        boolean isValidTransition = false;

        switch (normalizedCurrentStatus) {
            case "DRAFT":
                if (newStatus.equals("REVIEWING")) isValidTransition = true;
                break;
            case "REVIEWING":
                if (newStatus.equals("APPROVED") || newStatus.equals("REJECTED")) isValidTransition = true;
                break;
            case "REJECTED":
                if (newStatus.equals("DRAFT")) isValidTransition = true;
                break;
            case "APPROVED":
                if (newStatus.equals("ONGOING")) isValidTransition = true;
                break;
            case "ONGOING":
                if (newStatus.equals("COMPLETED") || newStatus.equals("HIATUS")) isValidTransition = true;
                break;
            case "HIATUS":
                if (newStatus.equals("ONGOING") || newStatus.equals("CANCELLED")) isValidTransition = true;
                break;
            case "COMPLETED":
            case "CANCELLED":
                isValidTransition = false;
                break;
            default:
                if (newStatus.equals("DRAFT")) isValidTransition = true;
        }

        if (!isValidTransition) {
            throw new RuntimeException("Error: Invalid state transition! Cannot move from " 
                    + normalizedCurrentStatus + " to " + newStatus);
        }

        series.setStatus(newStatus);
        MangaSeries updatedSeries = mangaSeriesRepository.save(series);
        
        return mapToResponse(updatedSeries);
    }

    // [BỔ SUNG] Cập nhật thông tin (Metadata)
    @Override
    @Transactional
    public MangaSeriesResponse updateSeriesMetadata(Long seriesId, Long currentUserId, MangaSeriesUpdateRequest request) {
        MangaSeries series = mangaSeriesRepository.findById(seriesId)
                .orElseThrow(() -> new RuntimeException("Error: Manga Series not found"));

        // Xác thực quyền sở hữu
        if (!series.getMangaka().getId().equals(currentUserId)) {
            throw new RuntimeException("Error: You do not have permission to modify this series");
        }

        if (request.getTitle() != null && !request.getTitle().isBlank()) series.setTitle(request.getTitle());
        if (request.getGenre() != null) series.setGenre(request.getGenre());
        if (request.getSummary() != null) series.setSummary(request.getSummary());
        if (request.getDescription() != null) series.setDescription(request.getDescription());
        String coverImageUrl = firstNonBlank(request.getCoverImageUrl(), request.getCoverUrl(), request.getImageUrl(), request.getThumbnailUrl(), request.getCoverImage(), request.getPrimaryArtUrl());
        if (coverImageUrl != null) series.setCoverImageUrl(coverImageUrl);

        MangaSeries updatedSeries = mangaSeriesRepository.save(series);
        return mapToResponse(updatedSeries);
    }

    // [BỔ SUNG] Xóa dự án truyện
    @Override
    @Transactional
    public void deleteSeries(Long seriesId, Long currentUserId) {
        MangaSeries series = mangaSeriesRepository.findById(seriesId)
                .orElseThrow(() -> new RuntimeException("Error: Manga Series not found"));

        // Xác thực quyền sở hữu
        if (!series.getMangaka().getId().equals(currentUserId)) {
            throw new RuntimeException("Error: You do not have permission to delete this series");
        }
        
        // Khóa an toàn: Chỉ được xóa bản nháp
        if (!"DRAFT".equals(series.getStatus())) {
            throw new RuntimeException("Error: Cannot delete series. Only projects in 'DRAFT' status can be deleted.");
        }

        mangaSeriesRepository.delete(series);
    }

    private String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private MangaSeriesResponse mapToResponse(MangaSeries series) {
        String tantouName = (series.getTantou() != null) ? series.getTantou().getFullName() : "Unassigned";

        return MangaSeriesResponse.builder()
                .id(series.getId())
                .title(series.getTitle())
                .genre(series.getGenre())
                .summary(series.getSummary())
                .description(series.getDescription())
                .coverImageUrl(series.getCoverImageUrl())
                .coverUrl(series.getCoverImageUrl())
                .imageUrl(series.getCoverImageUrl())
                .thumbnailUrl(series.getCoverImageUrl())
                .status(series.getStatus())
                .mangakaId(series.getMangaka().getId())
                .mangakaUsername(series.getMangaka().getUsername())
                .mangakaEmail(series.getMangaka().getEmail())
                .mangakaName(series.getMangaka().getFullName())
                .tantouName(tantouName)
                .createdAt(series.getCreatedAt())
                .build();
    }
    // [FE-17] Triển khai logic phán quyết của Admin
    @Override
    @Transactional
    public MangaSeriesResponse adminApproveSeries(Long seriesId, boolean isApproved) {
        MangaSeries series = mangaSeriesRepository.findById(seriesId)
                .orElseThrow(() -> new RuntimeException("Error: Manga Series not found"));

        // Khóa bảo vệ 1: Truyện phải đang ở giai đoạn REVIEWING mới được xét duyệt
        if (!"REVIEWING".equalsIgnoreCase(series.getStatus())) {
            throw new RuntimeException("Error: The series must be in 'REVIEWING' status for Admin decision.");
        }

        // Khóa bảo vệ 2: Nếu Admin bấm Đồng ý (true), kiểm tra xem Hội đồng có phiếu thuận nào không
        if (isApproved) {
            long approvedVotes = boardVoteRepository.countByMangaSeriesIdAndIsApproved(seriesId, true);
            if (approvedVotes == 0) {
                throw new RuntimeException("Error: Cannot approve! The Editorial Board has cast 0 approval votes for this project.");
            }
        }

        // Chốt trạng thái: Đồng ý -> APPROVED | Từ chối -> REJECTED
        series.setStatus(isApproved ? "APPROVED" : "REJECTED");
        MangaSeries updatedSeries = mangaSeriesRepository.save(series);

        return mapToResponse(updatedSeries);
    }

    @Override
    @Transactional
    public MangaSeries handleAdminDecision(Long seriesId, Boolean isApproved, Long tantouId) {
        MangaSeries series = mangaSeriesRepository.findById(seriesId)
                .orElseThrow(() -> new RuntimeException("Error: Manga Series không tồn tại"));

        if (isApproved) {
            series.setStatus("APPROVED");

            // LOGIC MỚI: Nếu Admin truyền lên ID của Biên tập viên, thực hiện gán ngay!
            if (tantouId != null) {
                User tantouEditor = userRepository.findById(tantouId)
                        .orElseThrow(() -> new RuntimeException("Error: Tài khoản Tantou Editor không tồn tại"));
                series.setTantou(tantouEditor);
            }
        } else {
            series.setStatus("REJECTED");
        }

        return mangaSeriesRepository.save(series);
    }

    @Override
    public Page<MangaSeriesResponse> getSeriesByStatus(String status, int page, int size) {
        // Sắp xếp truyện mới nhất lên đầu (theo createdAt giảm dần)
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        Page<MangaSeries> seriesPage;
        if (status != null && !status.isBlank()) {
            seriesPage = mangaSeriesRepository.findByStatus(status.toUpperCase(), pageable);
        } else {
            // Nếu Frontend không truyền status, mặc định lấy toàn bộ sàn
            seriesPage = mangaSeriesRepository.findAll(pageable);
        }

        // Chuyển đổi từ Page<MangaSeries> sang Page<MangaSeriesResponse>
        return seriesPage.map(this::mapToResponse);
    }

}