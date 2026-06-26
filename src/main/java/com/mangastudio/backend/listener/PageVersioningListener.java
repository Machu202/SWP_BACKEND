package com.mangastudio.backend.listener;

import com.mangastudio.backend.entity.Page;
import com.mangastudio.backend.entity.PageVersion;
import com.mangastudio.backend.repository.PageVersionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import java.time.LocalDateTime;

@Component
public class PageVersioningListener {

    @Autowired
    @Lazy
    private PageVersionRepository pageVersionRepository;
    
    @PostPersist
    @PostUpdate
    public void savePageSnapshot(Page page) {
        
        PageVersion newVersion = new PageVersion();
        newVersion.setPage(page);
        newVersion.setImageUrl(page.getImageUrl());
        newVersion.setCreatedAt(LocalDateTime.now());

        // BỔ SUNG: Logic tự động đếm số lượng phiên bản hiện có và cộng thêm 1
        // Ví dụ: Đã có 2 bản, thì bản mới lưu này sẽ là version_number = 3
        try {
            int currentVersionsCount = pageVersionRepository.countByPageId(page.getId());
            newVersion.setVersionNumber(currentVersionsCount + 1);
        } catch (Exception e) {
            // Nếu có lỗi lúc đếm (hoặc chưa có hàm), mặc định là 1
            newVersion.setVersionNumber(1);
        }

        pageVersionRepository.save(newVersion);
        
        System.out.println(">>> [VERSIONING LISTENER] Automatically backed up Version " + newVersion.getVersionNumber() + " for Page ID: " + page.getId());
    }
}