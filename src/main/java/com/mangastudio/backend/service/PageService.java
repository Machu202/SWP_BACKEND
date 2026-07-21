package com.mangastudio.backend.service;

import com.mangastudio.backend.entity.Page;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface PageService {
    Page addPageToChapter(Long chapterId, Integer pageNumber, MultipartFile file, Long currentUserId);
    
    // [FE-43] Updating an image creates a version-history entry.
    Page updatePageImage(Long pageId, MultipartFile newFile, Long currentUserId);
    
    List<Page> getPagesByChapter(Long chapterId);
    void deletePage(Long pageId, Long currentUserId);
}
