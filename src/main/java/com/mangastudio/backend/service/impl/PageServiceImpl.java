package com.mangastudio.backend.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.mangastudio.backend.entity.Chapter;
import com.mangastudio.backend.entity.Page;
import com.mangastudio.backend.entity.PageVersion;
import com.mangastudio.backend.repository.ChapterRepository;
import com.mangastudio.backend.repository.PageRepository;
import com.mangastudio.backend.repository.PageVersionRepository;
import com.mangastudio.backend.service.PageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
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
            Page savedPage = pageRepository.saveAndFlush(page);

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
            Page updatedPage = pageRepository.saveAndFlush(page);

            // 3. [FE-43] Find the latest version number and create the next snapshot.
            int nextVersionNumber = getNextVersionNumber(updatedPage.getId());
            saveVersionSnapshot(updatedPage, newFileUrl, nextVersionNumber);

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
    private int getNextVersionNumber(Long pageId) {
        return pageVersionRepository.findTopByPageIdOrderByVersionNumberDesc(pageId)
                .map(PageVersion::getVersionNumber)
                .map(version -> version + 1)
                .orElse(1);
    }

    private void saveVersionSnapshot(Page savedPage, String imgUrl, int verNum) {
        if (savedPage == null || savedPage.getId() == null) {
            throw new IllegalStateException("Cannot create PageVersion before Page has been saved.");
        }

        Page managedPage = pageRepository.getReferenceById(savedPage.getId());

        PageVersion version = PageVersion.builder()
                .page(managedPage)
                .imageUrl(imgUrl)
                .versionNumber(verNum)
                .createdAt(LocalDateTime.now())
                .build();

        pageVersionRepository.saveAndFlush(version);

        System.out.println(">>> [FE-43 VERSIONING] Successfully archived Snapshot Version "
                + verNum + " for Page ID: " + savedPage.getId());
    }
}