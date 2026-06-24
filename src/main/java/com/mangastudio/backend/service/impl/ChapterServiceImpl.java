package com.mangastudio.backend.service.impl;

import com.mangastudio.backend.dto.request.ChapterCreateRequest;
import com.mangastudio.backend.dto.response.ChapterResponse;
import com.mangastudio.backend.entity.Chapter;
import com.mangastudio.backend.entity.MangaSeries;
import com.mangastudio.backend.repository.ChapterRepository;
import com.mangastudio.backend.repository.MangaSeriesRepository;
import com.mangastudio.backend.service.ChapterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChapterServiceImpl implements ChapterService {

    private final ChapterRepository chapterRepository;
    private final MangaSeriesRepository mangaSeriesRepository;

    @Override
    @Transactional
    public ChapterResponse createChapter(Long currentUserId, ChapterCreateRequest request) {
        MangaSeries series = mangaSeriesRepository.findById(request.getSeriesId())
                .orElseThrow(() -> new RuntimeException("Error: Manga Series not found"));

        // Lỗ hổng bảo mật: Xác nhận chỉ chủ sở hữu truyện mới được tạo Chapter
        if (!series.getMangaka().getId().equals(currentUserId)) {
            throw new RuntimeException("Error: You do not have permission to add chapters to this series");
        }

        Chapter chapter = Chapter.builder()
                .mangaSeries(series)
                .chapterNumber(request.getChapterNumber())
                .title(request.getTitle())
                .publishStatus("DRAFT") // Mặc định tạo ra luôn là bản nháp
                .build();

        Chapter savedChapter = chapterRepository.save(chapter);
        return mapToResponse(savedChapter);
    }

    @Override
    public ChapterResponse getChapterById(Long chapterId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("Error: Chapter not found"));
        return mapToResponse(chapter);
    }

    @Override
    public List<ChapterResponse> getAllChaptersBySeries(Long seriesId) {
        return chapterRepository.findByMangaSeriesIdOrderByChapterNumberAsc(seriesId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // [FE-30] State Machine cho Chapter
    @Override
    @Transactional
    public ChapterResponse updateChapterStatus(Long chapterId, Long currentUserId, String newStatusStr) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("Error: Chapter not found"));

        if (!chapter.getMangaSeries().getMangaka().getId().equals(currentUserId)) {
            throw new RuntimeException("Error: You do not have permission to update this chapter");
        }

        String currentStatus = chapter.getPublishStatus();
        String newStatus = newStatusStr.toUpperCase();
        String normalizedCurrentStatus = (currentStatus != null) ? currentStatus.toUpperCase() : "DRAFT";

        boolean isValidTransition = false;
        switch (normalizedCurrentStatus) {
            case "DRAFT":
                // Bản nháp có thể Lên lịch hoặc Xuất bản ngay
                if (newStatus.equals("SCHEDULED") || newStatus.equals("PUBLISHED")) isValidTransition = true;
                break;
            case "SCHEDULED":
                // Đã lên lịch có thể Xuất bản hoặc Hủy lịch quay về Nháp
                if (newStatus.equals("PUBLISHED") || newStatus.equals("DRAFT")) isValidTransition = true;
                break;
            case "PUBLISHED":
                // Đã xuất bản thì chỉ có thể Lưu trữ (Cất đi, không hiển thị nữa)
                if (newStatus.equals("ARCHIVED")) isValidTransition = true;
                break;
            case "ARCHIVED":
                // Từ lưu trữ có thể khôi phục lại trạng thái Xuất bản
                if (newStatus.equals("PUBLISHED")) isValidTransition = true; 
                break;
            default:
                if (newStatus.equals("DRAFT")) isValidTransition = true;
        }

        if (!isValidTransition) {
            throw new RuntimeException("Error: Invalid state transition for Chapter! Cannot move from " 
                    + normalizedCurrentStatus + " to " + newStatus);
        }

        chapter.setPublishStatus(newStatus);
        Chapter updatedChapter = chapterRepository.save(chapter);
        return mapToResponse(updatedChapter);
    }

    @Override
    @Transactional
    public void deleteChapter(Long chapterId, Long currentUserId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("Error: Chapter not found"));

        if (!chapter.getMangaSeries().getMangaka().getId().equals(currentUserId)) {
            throw new RuntimeException("Error: You do not have permission to delete this chapter");
        }

        // Khóa an toàn: Chỉ cho xóa khi chương còn là bản nháp
        if (!"DRAFT".equals(chapter.getPublishStatus().toUpperCase())) {
            throw new RuntimeException("Error: Cannot delete. Only DRAFT chapters can be deleted.");
        }

        chapterRepository.delete(chapter);
    }

    private ChapterResponse mapToResponse(Chapter chapter) {
        return ChapterResponse.builder()
                .id(chapter.getId())
                .seriesId(chapter.getMangaSeries().getId())
                .seriesTitle(chapter.getMangaSeries().getTitle())
                .chapterNumber(chapter.getChapterNumber())
                .title(chapter.getTitle())
                .publishStatus(chapter.getPublishStatus())
                .build();
    }
}