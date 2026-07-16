package com.mangastudio.backend.service.impl;

import com.mangastudio.backend.dto.request.MangaSeriesCreateRequest;
import com.mangastudio.backend.dto.request.MangaSeriesUpdateRequest;
import com.mangastudio.backend.dto.response.MangaSeriesResponse;
import com.mangastudio.backend.entity.Chapter;
import com.mangastudio.backend.entity.MangaSeries;
import com.mangastudio.backend.entity.User;
import com.mangastudio.backend.repository.ChapterRepository;
import com.mangastudio.backend.repository.MangaSeriesRepository;
import com.mangastudio.backend.repository.UserRepository;
import com.mangastudio.backend.service.MangaSeriesService;
import com.mangastudio.backend.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import com.mangastudio.backend.repository.BoardVoteRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
@Service
public class MangaSeriesServiceImpl implements MangaSeriesService {

    private final MangaSeriesRepository mangaSeriesRepository;
    private final UserRepository userRepository;
    private final BoardVoteRepository boardVoteRepository;
    private final ChapterRepository chapterRepository;
    private final NotificationService notificationService;

    public MangaSeriesServiceImpl(MangaSeriesRepository mangaSeriesRepository,
                                  UserRepository userRepository,
                                  BoardVoteRepository boardVoteRepository,
                                  ChapterRepository chapterRepository,
                                  NotificationService notificationService) {
        this.mangaSeriesRepository = mangaSeriesRepository;
        this.userRepository = userRepository;
        this.boardVoteRepository = boardVoteRepository;
        this.chapterRepository = chapterRepository;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public MangaSeriesResponse createSeries(Long mangakaId, MangaSeriesCreateRequest request) {
        User mangaka = userRepository.findById(mangakaId)
                .orElseThrow(() -> new RuntimeException("Error: Mangaka not found with ID: " + mangakaId));

        if (!hasRole(mangaka, "MANGAKA")) {
            throw new AccessDeniedException("Only Mangaka users can create manga series.");
        }

        MangaSeries newSeries = MangaSeries.builder()
                .mangaka(mangaka)
                .title(request.getTitle())
                .genre(request.getGenre())
                .summary(request.getSummary())
                .description(request.getDescription())
                .coverImageUrl(request.getCoverImageUrl())
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
    public List<MangaSeriesResponse> getAllSeriesAssignedToTantou(Long tantouId) {
        User tantou = userRepository.findById(tantouId)
                .orElseThrow(() -> new RuntimeException("Error: Tantou Editor not found"));
        if (!hasRole(tantou, "TANTOU EDITOR")) {
            throw new AccessDeniedException("Only Tantou Editors can read assigned series.");
        }
        return mangaSeriesRepository.findAssignedToTantou(tantouId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MangaSeriesResponse updateSeriesStatus(Long seriesId, Long currentUserId, String newStatusStr) {
        MangaSeries series = mangaSeriesRepository.findById(seriesId)
                .orElseThrow(() -> new RuntimeException("Error: Manga Series not found"));

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Error: User not found"));

        if (!hasRole(currentUser, "MANGAKA") || !series.getMangaka().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not have permission to change this series status.");
        }

        String currentStatus = series.getStatus();
        String newStatus = newStatusStr.toUpperCase();
        String normalizedCurrentStatus = (currentStatus != null) ? currentStatus.toUpperCase() : "DRAFT";

        boolean isValidTransition = false;

        switch (normalizedCurrentStatus) {
            case "DRAFT":
                if (newStatus.equals("REVIEWING")) isValidTransition = true;
                break;
            case "REVIEWING":
                // Final approval/rejection is exclusively handled by the Admin decision endpoint.
                isValidTransition = false;
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

        if ("REVIEWING".equals(newStatus)) {
            validateAllChaptersApprovedForBoard(series);
            // A rejected series may be resubmitted after returning to DRAFT. Old votes
            // must never count toward a new Editorial Board review cycle.
            resetBoardVotesForNewReviewCycle(seriesId);
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

        // Ownership violations are authorization failures, not malformed requests.
        if (!series.getMangaka().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not have permission to modify this series.");
        }

        if (request.getTitle() != null && !request.getTitle().isBlank()) series.setTitle(request.getTitle());
        if (request.getGenre() != null) series.setGenre(request.getGenre());
        if (request.getSummary() != null) series.setSummary(request.getSummary());
        if (request.getDescription() != null) series.setDescription(request.getDescription());
        if (request.getCoverImageUrl() != null && !request.getCoverImageUrl().isBlank()) series.setCoverImageUrl(request.getCoverImageUrl());

        MangaSeries updatedSeries = mangaSeriesRepository.save(series);
        return mapToResponse(updatedSeries);
    }

    // [BỔ SUNG] Xóa dự án truyện
    @Override
    @Transactional
    public void deleteSeries(Long seriesId, Long currentUserId) {
        MangaSeries series = mangaSeriesRepository.findById(seriesId)
                .orElseThrow(() -> new RuntimeException("Error: Manga Series not found"));

        // Ownership violations are authorization failures, not malformed requests.
        if (!series.getMangaka().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not have permission to delete this series.");
        }
        
        // Khóa an toàn: Chỉ được xóa bản nháp
        if (!"DRAFT".equals(series.getStatus())) {
            throw new RuntimeException("Error: Cannot delete series. Only projects in 'DRAFT' status can be deleted.");
        }

        mangaSeriesRepository.delete(series);
    }

    private MangaSeriesResponse mapToResponse(MangaSeries series) {
        String tantouName = (series.getTantou() != null) ? series.getTantou().getFullName() : "Unassigned";

        return MangaSeriesResponse.builder()
                .id(series.getId())
                .displayNumber(series.getId() != null ? mangaSeriesRepository.countByIdLessThanEqual(series.getId()) : null)
                .title(series.getTitle())
                .genre(series.getGenre())
                .summary(series.getSummary())
                .description(series.getDescription())
                .coverImageUrl(series.getCoverImageUrl())
                .status(series.getStatus())
                .mangakaName(series.getMangaka().getFullName())
                .tantouId(series.getTantou() != null ? series.getTantou().getId() : null)
                .tantouName(tantouName)
                .createdAt(series.getCreatedAt())
                .build();
    }
    @Override
    @Transactional
    public MangaSeriesResponse assignTantou(Long seriesId, Long currentUserId, Long tantouId) {
        MangaSeries series = mangaSeriesRepository.findById(seriesId)
                .orElseThrow(() -> new RuntimeException("Error: Manga Series not found"));
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Error: User not found"));

        boolean ownerMangaka = hasRole(currentUser, "MANGAKA")
                && series.getMangaka() != null
                && series.getMangaka().getId().equals(currentUserId);
        boolean admin = hasRole(currentUser, "ADMIN");
        if (!ownerMangaka && !admin) {
            throw new AccessDeniedException("You do not have permission to assign a Tantou Editor to this series.");
        }

        String status = series.getStatus() == null ? "DRAFT" : series.getStatus().trim().toUpperCase();
        if (status.equals("CANCELLED") || status.equals("COMPLETED") || status.equals("ARCHIVED")) {
            throw new RuntimeException("Error: A Tantou Editor cannot be assigned to a closed series.");
        }

        User tantou = userRepository.findById(tantouId)
                .orElseThrow(() -> new RuntimeException("Error: Tantou Editor not found"));
        if (!hasRole(tantou, "TANTOU EDITOR")) {
            throw new RuntimeException("Error: The assigned user must have the Tantou Editor role.");
        }

        Long previousTantouId = series.getTantou() != null ? series.getTantou().getId() : null;
        series.setTantou(tantou);
        MangaSeries savedSeries = mangaSeriesRepository.save(series);

        if (ownerMangaka && !tantou.getId().equals(previousTantouId)) {
            String seriesTitle = savedSeries.getTitle() == null || savedSeries.getTitle().isBlank()
                    ? "Manga Series"
                    : savedSeries.getTitle();
            notificationService.createNotification(
                    tantou.getId(),
                    "\"" + seriesTitle + "\" Mangaka has assigned you to review!",
                    "/series");
        }
        return mapToResponse(savedSeries);
    }

    @Override
    @Transactional
    public MangaSeriesResponse submitToEditorialBoard(Long seriesId, Long currentUserId) {
        MangaSeries series = mangaSeriesRepository.findById(seriesId)
                .orElseThrow(() -> new RuntimeException("Error: Manga Series not found"));
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Error: User not found"));

        boolean assignedTantou = hasRole(currentUser, "TANTOU EDITOR")
                && series.getTantou() != null
                && series.getTantou().getId().equals(currentUserId);
        if (!assignedTantou) {
            throw new AccessDeniedException("Only the Tantou Editor assigned to this series can submit it to the Editorial Board.");
        }

        String status = normalizeStatus(series.getStatus(), "DRAFT");
        if (!"DRAFT".equals(status)) {
            throw new RuntimeException("Error: Only a DRAFT series can be submitted to the Editorial Board. Current status: " + status + ".");
        }

        validateAllChaptersApprovedForBoard(series);
        resetBoardVotesForNewReviewCycle(seriesId);
        series.setStatus("REVIEWING");
        return mapToResponse(mangaSeriesRepository.save(series));
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
                .orElseThrow(() -> new RuntimeException("Error: Manga Series does not exist."));

        if (!"REVIEWING".equalsIgnoreCase(series.getStatus())) {
            throw new RuntimeException("Error: The series must be in 'REVIEWING' status for an Admin decision.");
        }

        if (Boolean.TRUE.equals(isApproved)) {
            long approvedVotes = boardVoteRepository.countByMangaSeriesIdAndIsApproved(seriesId, true);
            if (approvedVotes == 0) {
                throw new RuntimeException("Error: Cannot approve a series without at least one Editorial Board approval vote.");
            }

            if (tantouId != null) {
                User tantouEditor = userRepository.findById(tantouId)
                        .orElseThrow(() -> new RuntimeException("Error: Tantou Editor account does not exist."));
                if (!hasRole(tantouEditor, "TANTOU EDITOR")) {
                    throw new RuntimeException("Error: The assigned user must have the Tantou Editor role.");
                }
                series.setTantou(tantouEditor);
            }
            series.setStatus("APPROVED");
        } else {
            series.setStatus("REJECTED");
        }

        return mangaSeriesRepository.save(series);
    }

    /**
     * A resubmission is a new Editorial Board review cycle. Votes from a prior
     * rejection/decision must never be reused for the edited draft.
     */
    private void resetBoardVotesForNewReviewCycle(Long seriesId) {
        boardVoteRepository.deleteByMangaSeriesId(seriesId);
    }

    private void validateAllChaptersApprovedForBoard(MangaSeries series) {
        if (series.getTantou() == null) {
            throw new RuntimeException("Error: Assign a Tantou Editor before Editorial Board review.");
        }

        List<Chapter> chapters = chapterRepository.findByMangaSeriesIdOrderByChapterNumberAsc(series.getId());
        if (chapters.isEmpty()) {
            throw new RuntimeException("Error: Create at least one chapter before Editorial Board review.");
        }

        List<Chapter> unfinished = chapters.stream()
                .filter(chapter -> !"APPROVED".equals(normalizeStatus(chapter.getPublishStatus(), "DRAFT")))
                .toList();
        if (!unfinished.isEmpty()) {
            String chapterNumbers = unfinished.stream()
                    .map(chapter -> chapter.getChapterNumber() == null
                            ? String.valueOf(chapter.getId())
                            : String.valueOf(chapter.getChapterNumber()))
                    .collect(Collectors.joining(", "));
            throw new RuntimeException("Error: Every chapter must be APPROVED by the assigned Tantou before Editorial Board review. Pending chapter(s): " + chapterNumbers + ".");
        }
    }

    private String normalizeStatus(String value, String fallback) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return normalized.isBlank() ? fallback : normalized;
    }

    private boolean hasRole(User user, String expectedRole) {
        return user != null
                && user.getRole() != null
                && user.getRole().getRoleName() != null
                && user.getRole().getRoleName().trim().equalsIgnoreCase(expectedRole);
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