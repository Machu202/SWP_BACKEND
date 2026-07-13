package com.mangastudio.backend.service.impl;

import com.mangastudio.backend.dto.request.ChapterCreateRequest;
import com.mangastudio.backend.dto.response.ChapterResponse;
import com.mangastudio.backend.entity.Chapter;
import com.mangastudio.backend.entity.MangaSeries;
import com.mangastudio.backend.entity.User;
import com.mangastudio.backend.repository.ChapterRepository;
import com.mangastudio.backend.repository.MangaSeriesRepository;
import com.mangastudio.backend.repository.TaskRepository;
import com.mangastudio.backend.repository.UserRepository;
import com.mangastudio.backend.service.ChapterService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ChapterServiceImpl implements ChapterService {

    private static final Set<String> TANTOU_REVIEW_STATES = Set.of(
            "REVIEWING", "IN_REVIEW", "PENDING_REVIEW", "MANGAKA_APPROVED",
            "READY_FOR_TANTOU", "TANTOU_REVIEW"
    );

    private final ChapterRepository chapterRepository;
    private final MangaSeriesRepository mangaSeriesRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;

    public ChapterServiceImpl(ChapterRepository chapterRepository,
                              MangaSeriesRepository mangaSeriesRepository,
                              UserRepository userRepository,
                              TaskRepository taskRepository) {
        this.chapterRepository = chapterRepository;
        this.mangaSeriesRepository = mangaSeriesRepository;
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
    }

    @Override
    @Transactional
    public ChapterResponse createChapter(Long currentUserId, ChapterCreateRequest request) {
        MangaSeries series = mangaSeriesRepository.findById(request.getSeriesId())
                .orElseThrow(() -> new RuntimeException("Error: Manga Series not found"));

        if (!series.getMangaka().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not have permission to add chapters to this series.");
        }

        Chapter chapter = Chapter.builder()
                .mangaSeries(series)
                .chapterNumber(request.getChapterNumber())
                .title(request.getTitle())
                .publishStatus("DRAFT")
                .build();

        return mapToResponse(chapterRepository.save(chapter), false);
    }

    @Override
    public ChapterResponse getChapterById(Long chapterId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("Error: Chapter not found"));
        return mapToResponse(chapter, false);
    }

    @Override
    public List<ChapterResponse> getAllChaptersBySeries(Long seriesId) {
        return chapterRepository.findByMangaSeriesIdOrderByChapterNumberAsc(seriesId).stream()
                .map(chapter -> mapToResponse(chapter, false))
                .collect(Collectors.toList());
    }

    @Override
    public List<ChapterResponse> getTantouReviewQueue(Long currentUserId) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Error: User not found"));

        if (!hasRole(currentUser, "TANTOU EDITOR")) {
            throw new AccessDeniedException("Only Tantou Editors can read the chapter review queue.");
        }

        return chapterRepository.findByMangaSeries_Tantou_IdOrderByChapterNumberAsc(currentUserId).stream()
                .filter(this::isVisibleInTantouQueue)
                .map(chapter -> {
                    boolean legacyReady = isLegacyApprovedTaskReview(chapter);
                    return mapToResponse(chapter, legacyReady);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ChapterResponse updateChapterStatus(Long chapterId, Long currentUserId, String newStatusStr) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("Error: Chapter not found"));
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Error: User not found"));

        MangaSeries series = chapter.getMangaSeries();
        boolean ownerMangaka = hasRole(currentUser, "MANGAKA")
                && series.getMangaka() != null
                && series.getMangaka().getId().equals(currentUserId);
        boolean assignedTantou = hasRole(currentUser, "TANTOU EDITOR")
                && series.getTantou() != null
                && series.getTantou().getId().equals(currentUserId);

        if (!ownerMangaka && !assignedTantou) {
            throw new AccessDeniedException("You do not have permission to update this chapter.");
        }

        String currentStatus = normalizeStatus(chapter.getPublishStatus(), "DRAFT");
        String newStatus = normalizeStatus(newStatusStr, "");
        boolean valid;

        if (ownerMangaka) {
            if ("REVIEWING".equals(newStatus)) {
                validateChapterReadyForTantou(chapter);
            }
            valid = isValidMangakaTransition(currentStatus, newStatus);
        } else {
            boolean legacyReady = "DRAFT".equals(currentStatus) && isLegacyApprovedTaskReview(chapter);
            valid = isValidTantouTransition(currentStatus, newStatus, legacyReady);
        }

        if (!valid) {
            throw new RuntimeException("Error: Invalid chapter state transition from "
                    + currentStatus + " to " + newStatus + ".");
        }

        chapter.setPublishStatus(newStatus);
        return mapToResponse(chapterRepository.save(chapter), false);
    }

    @Override
    @Transactional
    public void deleteChapter(Long chapterId, Long currentUserId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("Error: Chapter not found"));

        if (!chapter.getMangaSeries().getMangaka().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not have permission to delete this chapter.");
        }

        if (!"DRAFT".equals(normalizeStatus(chapter.getPublishStatus(), "DRAFT"))) {
            throw new RuntimeException("Error: Cannot delete. Only DRAFT chapters can be deleted.");
        }

        chapterRepository.delete(chapter);
    }

    private void validateChapterReadyForTantou(Chapter chapter) {
        MangaSeries series = chapter.getMangaSeries();
        if (series == null || series.getTantou() == null) {
            throw new RuntimeException("Error: Assign a Tantou Editor before sending this chapter for review.");
        }

        long totalTasks = taskRepository.countByHitbox_Page_Chapter_Id(chapter.getId());
        long approvedTasks = taskRepository.countByHitbox_Page_Chapter_IdAndStatusIgnoreCase(chapter.getId(), "APPROVED");
        if (totalTasks == 0) {
            throw new RuntimeException("Error: This chapter has no Assistant tasks to review.");
        }
        if (approvedTasks != totalTasks) {
            throw new RuntimeException("Error: All Assistant tasks in this chapter must be APPROVED before Tantou review.");
        }
    }

    private boolean isValidMangakaTransition(String currentStatus, String newStatus) {
        return switch (currentStatus) {
            case "DRAFT" -> Set.of("REVIEWING", "SCHEDULED", "PUBLISHED").contains(newStatus);
            case "REVISION" -> Set.of("REVIEWING", "DRAFT").contains(newStatus);
            case "SCHEDULED" -> Set.of("PUBLISHED", "DRAFT").contains(newStatus);
            case "PUBLISHED" -> "ARCHIVED".equals(newStatus);
            case "ARCHIVED" -> "PUBLISHED".equals(newStatus);
            default -> false;
        };
    }

    private boolean isValidTantouTransition(String currentStatus, String newStatus, boolean legacyReady) {
        boolean reviewReady = TANTOU_REVIEW_STATES.contains(currentStatus) || legacyReady;
        return reviewReady && Set.of("APPROVED", "REVISION").contains(newStatus);
    }

    private boolean isVisibleInTantouQueue(Chapter chapter) {
        String status = normalizeStatus(chapter != null ? chapter.getPublishStatus() : null, "DRAFT");
        return TANTOU_REVIEW_STATES.contains(status)
                || Set.of("REVISION", "APPROVED", "PUBLISHED").contains(status)
                || isLegacyApprovedTaskReview(chapter);
    }

    private boolean isLegacyApprovedTaskReview(Chapter chapter) {
        return chapter != null
                && chapter.getId() != null
                && "DRAFT".equals(normalizeStatus(chapter.getPublishStatus(), "DRAFT"))
                && taskRepository.existsByHitbox_Page_Chapter_IdAndStatusIgnoreCase(chapter.getId(), "APPROVED");
    }

    private ChapterResponse mapToResponse(Chapter chapter, boolean legacyReviewReady) {
        MangaSeries series = chapter.getMangaSeries();
        User mangaka = series != null ? series.getMangaka() : null;
        User tantou = series != null ? series.getTantou() : null;
        String status = legacyReviewReady
                ? "READY_FOR_TANTOU"
                : normalizeStatus(chapter.getPublishStatus(), "DRAFT");

        return ChapterResponse.builder()
                .id(chapter.getId())
                .seriesId(series != null ? series.getId() : null)
                .seriesTitle(series != null ? series.getTitle() : null)
                .chapterNumber(chapter.getChapterNumber())
                .title(chapter.getTitle())
                .publishStatus(status)
                .mangakaName(displayName(mangaka))
                .tantouId(tantou != null ? tantou.getId() : null)
                .tantouName(displayName(tantou))
                .reviewReady(legacyReviewReady || TANTOU_REVIEW_STATES.contains(status))
                .build();
    }

    private String normalizeStatus(String status, String fallback) {
        String value = status == null ? "" : status.trim();
        if (value.isEmpty()) return fallback;
        return value.toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    }

    private boolean hasRole(User user, String roleName) {
        return user != null
                && user.getRole() != null
                && user.getRole().getRoleName() != null
                && user.getRole().getRoleName().trim().equalsIgnoreCase(roleName);
    }

    private String displayName(User user) {
        if (user == null) return null;
        if (user.getFullName() != null && !user.getFullName().isBlank()) return user.getFullName();
        if (user.getUsername() != null && !user.getUsername().isBlank()) return user.getUsername();
        if (user.getEmail() != null && !user.getEmail().isBlank()) return user.getEmail();
        return "User #" + user.getId();
    }
}
