package com.mangastudio.backend.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.mangastudio.backend.entity.Chapter;
import com.mangastudio.backend.entity.Page;
import com.mangastudio.backend.repository.ChapterRepository;
import com.mangastudio.backend.repository.PageRepository;
import com.mangastudio.backend.service.PageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PageServiceImpl implements PageService {

    private final PageRepository pageRepository;
    private final ChapterRepository chapterRepository;
    private final Cloudinary cloudinary;

    @Override
    @Transactional
    public Page addPageToChapter(Long chapterId, Integer pageNumber, MultipartFile file, Long currentUserId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("Error: Chapter not found"));

        // Xác thực quyền: Chỉ chủ sở hữu truyện mới được thêm trang
        if (!chapter.getMangaSeries().getMangaka().getId().equals(currentUserId)) {
            throw new RuntimeException("Error: You do not have permission to modify this chapter");
        }

        try {
            // Upload thẳng lên Cloudinary
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

            // Lưu lần đầu tiên -> Kích hoạt @PostPersist trong Listener
            return pageRepository.save(page);

        } catch (IOException ex) {
            throw new RuntimeException("Error: Failed to upload page image to Cloudinary", ex);
        }
    }

    @Override
    @Transactional
    public Page updatePageImage(Long pageId, MultipartFile newFile, Long currentUserId) {
        Page page = pageRepository.findById(pageId)
                .orElseThrow(() -> new RuntimeException("Error: Page not found"));

        // Xác thực quyền: Chỉ chủ sở hữu truyện mới được sửa trang
        if (!page.getChapter().getMangaSeries().getMangaka().getId().equals(currentUserId)) {
            throw new RuntimeException("Error: You do not have permission to update this page");
        }

        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(newFile.getBytes(), ObjectUtils.asMap(
                "resource_type", "auto",
                    "folder", "mangastudio_pages"
            ));
            String newFileUrl = uploadResult.get("secure_url").toString();

            // Ghi đè URL mới. Lệnh save() này sẽ lập tức bóp cò kích hoạt @PostUpdate trong PageVersioningListener (FE-43)
            page.setImageUrl(newFileUrl);
            return pageRepository.save(page);

        } catch (IOException ex) {
            throw new RuntimeException("Error: Failed to upload new page image", ex);
        }
    }

    @Override
    public List<Page> getPagesByChapter(Long chapterId) {
        // Kiểm tra chapter có tồn tại không
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

        // Lưu ý: Tạm thời chỉ xóa Database. 
        // Nếu muốn xóa sạch rác trên Cloudinary, cần lưu thêm biến public_id vào Page tương tự như cách đã làm ở Resource.
        pageRepository.delete(page);
    }
}