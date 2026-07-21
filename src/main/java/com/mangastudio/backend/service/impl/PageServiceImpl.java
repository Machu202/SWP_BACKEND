package com.mangastudio.backend.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.mangastudio.backend.entity.Chapter;
import com.mangastudio.backend.entity.Page;
import com.mangastudio.backend.entity.PageVersion;
import com.mangastudio.backend.entity.SystemParameter;
import com.mangastudio.backend.repository.ChapterRepository;
import com.mangastudio.backend.repository.PageRepository;
import com.mangastudio.backend.repository.PageVersionRepository;
import com.mangastudio.backend.repository.SystemParameterRepository;
import com.mangastudio.backend.service.PageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PageServiceImpl implements PageService {

    private static final String MAX_PAGES_PER_CHAPTER = "MAX_PAGES_PER_CHAPTER";

    private final PageRepository pageRepository;
    private final ChapterRepository chapterRepository;
    private final Cloudinary cloudinary;
    private final PageVersionRepository pageVersionRepository;
    private final SystemParameterRepository systemParameterRepository;

    @Override
    @Transactional
    public Page addPageToChapter(Long chapterId, Integer pageNumber, MultipartFile file, Long currentUserId) {
        Chapter chapter = chapterRepository.findByIdForPageUpload(chapterId)
                .orElseThrow(() -> new RuntimeException("Error: Chapter not found"));
        if (!chapter.getMangaSeries().getMangaka().getId().equals(currentUserId)) {
            throw new RuntimeException("Error: You do not have permission to modify this chapter");
        }
        if (pageNumber == null || pageNumber < 1) {
            throw new RuntimeException("Error: Page number must be greater than zero");
        }
        if (pageRepository.existsByChapterIdAndPageNumber(chapterId, pageNumber)) {
            throw new RuntimeException("Page number is unique");
        }
        enforceConfiguredPageLimit(chapterId);

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

            // [FE-43] Archive version 1 when the page is uploaded for the first time.
            saveVersionSnapshot(savedPage, fileUrl, 1);

            return savedPage;
        } catch (IOException ex) {
            throw new RuntimeException("Error: Failed to upload page image to Cloudinary", ex);
        } catch (DataIntegrityViolationException ex) {
            throw new RuntimeException("Page number is unique", ex);
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
            // 1. Upload the replacement image to Cloudinary.
            Map<?, ?> uploadResult = cloudinary.uploader().upload(newFile.getBytes(), ObjectUtils.asMap(
                "resource_type", "auto",
                "folder", "mangastudio_pages"
            ));
            String newFileUrl = uploadResult.get("secure_url").toString();

            // 2. Update the current image on the page.
            page.setImageUrl(newFileUrl);
            Page updatedPage = pageRepository.save(page);

            // 3. [FE-43] Count archived versions and create the next snapshot.
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

    private void enforceConfiguredPageLimit(Long chapterId) {
        SystemParameter parameter = systemParameterRepository
                .findByParamKeyIgnoreCase(MAX_PAGES_PER_CHAPTER)
                .orElse(null);
        if (parameter == null) return;

        int maximum;
        try {
            // Parse legacy rows too; all new Admin writes are constrained to INTEGER by the settings service.
            maximum = Integer.parseInt(parameter.getParamValue().trim());
            if (maximum < 1) throw new NumberFormatException("value is not positive");
        } catch (RuntimeException exception) {
            throw new RuntimeException(
                    "Error: MAX_PAGES_PER_CHAPTER must be configured as a positive INTEGER.", exception);
        }

        long currentPageCount = pageRepository.countByChapterId(chapterId);
        if (currentPageCount >= maximum) {
            throw new RuntimeException(
                    "Error: This chapter has reached the Admin limit of " + maximum + " pages.");
        }
    }

    // Archives one immutable page-version snapshot.
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
