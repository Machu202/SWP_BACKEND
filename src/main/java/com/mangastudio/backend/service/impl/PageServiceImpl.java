package com.mangastudio.backend.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.mangastudio.backend.entity.Chapter;
import com.mangastudio.backend.entity.Page;
import com.mangastudio.backend.entity.PageVersion; // <-- Thêm
import com.mangastudio.backend.repository.ChapterRepository;
import com.mangastudio.backend.repository.PageRepository;
import com.mangastudio.backend.repository.PageVersionRepository; // <-- Thêm
import com.mangastudio.backend.service.PageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime; // <-- Thêm
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PageServiceImpl implements PageService {

    private final PageRepository pageRepository;
    private final ChapterRepository chapterRepository;
    private final Cloudinary cloudinary;
    private final PageVersionRepository pageVersionRepository; // Tiêm kho chứa lịch sử vào

    @Override
    @Transactional
    public Page addPageToChapter(Long chapterId, Integer pageNumber, MultipartFile file, Long currentUserId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("Error: Chapter not found"));
        if (!chapter.getMangaSeries().getMangaka().getId().equals(currentUserId)) {
            throw new RuntimeException("Error: You do not have permission to modify this chapter");
        }

        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "resource_type", "auto",
                    "folder", "mangastudio_pages"
            ));
            String fileUrl = uploadResult.get("secure_url").toString();

            Page page = Page.builder()
                    .chapter(chapter)
                    .pageNumber(pageNumber)
                    .imageUrl(fileUrl)
                    .build();
            Page savedPage = pageRepository.save(page);

            // [FE-43] Tạo mốc lịch sử Version 1 khi upload lần đầu
            saveVersionSnapshot(savedPage, fileUrl, 1);

            return savedPage;
        } catch (IOException ex) {
            throw new RuntimeException("Error: Failed to upload page image to Cloudinary", ex);
        }
    }

    @Override
    @Transactional
    public Page updatePageImage(Long pageId, MultipartFile newFile, Long currentUserId) {
        Page page = pageRepository.findById(pageId)
                .orElseThrow(() -> new RuntimeException("Error: Page not found"));
        if (!page.getChapter().getMangaSeries().getMangaka().getId().equals(currentUserId)) {
            throw new RuntimeException("Error: You do not have permission to update this page");
        }

        try {
            // 1. Upload ảnh mới đè lên Cloudinary
            Map<?, ?> uploadResult = cloudinary.uploader().upload(newFile.getBytes(), ObjectUtils.asMap(
                "resource_type", "auto",
                "folder", "mangastudio_pages"
            ));
            String newFileUrl = uploadResult.get("secure_url").toString();

            // 2. Cập nhật ảnh mới cho Page chính
            page.setImageUrl(newFileUrl);
            Page updatedPage = pageRepository.save(page);

            // 3. [FE-43] Đếm số phiên bản hiện có trong DB và tạo Snapshot mới (Version 2, 3...)
            int currentVersionCount = pageVersionRepository.countByPageId(pageId);
            saveVersionSnapshot(updatedPage, newFileUrl, currentVersionCount + 1);

            return updatedPage;

        } catch (IOException ex) {
            throw new RuntimeException("Error: Failed to upload new page image", ex);
        }
    }

    @Override
    public List<Page> getPagesByChapter(Long chapterId) {
        chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("Error: Chapter not found"));
        return pageRepository.findByChapterIdOrderByPageNumberAsc(chapterId);
    }

    @Override
    @Transactional
    public void deletePage(Long pageId, Long currentUserId) {
        Page page = pageRepository.findById(pageId)
                .orElseThrow(() -> new RuntimeException("Error: Page not found"));
        if (!page.getChapter().getMangaSeries().getMangaka().getId().equals(currentUserId)) {
            throw new RuntimeException("Error: You do not have permission to delete this page");
        }
        pageRepository.delete(page);
    }

    // --- Hàm trợ lý ghi vết Version ---
    private void saveVersionSnapshot(Page page, String imgUrl, int verNum) {
        PageVersion version = PageVersion.builder()
                .page(page)
                .imageUrl(imgUrl)
                .versionNumber(verNum)
                .createdAt(LocalDateTime.now())
                .build();
        pageVersionRepository.save(version);
        System.out.println(">>> [FE-43 VERSIONING] Successfully archived Snapshot Version " + verNum + " for Page ID: " + page.getId());
    }
}