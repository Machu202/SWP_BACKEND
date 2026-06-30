package com.mangastudio.backend.listener;

import org.springframework.stereotype.Component;

@Component
public class PageVersioningListener {
    // Đã vô hiệu hóa JPA Listener. Logic lưu vết bản thảo (FE-43) 
}

/*package com.mangastudio.backend.listener;

import com.mangastudio.backend.entity.Page;
import com.mangastudio.backend.entity.PageVersion;
import com.mangastudio.backend.repository.PageVersionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager; // <-- Nhớ import EntityManager
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import java.time.LocalDateTime;

@Component
public class PageVersioningListener {

    @Autowired
    @Lazy
    private PageVersionRepository pageVersionRepository;

    @Autowired
    @Lazy
    private EntityManager entityManager; // Tiêm cỗ máy quản lý thực thể của JPA vào

    // TUYỆT ĐỐI KHÔNG dùng REQUIRES_NEW! 
    // Dùng REQUIRED để PageVersion hòa chung vào Giao dịch A của PageServiceImpl
    @Transactional(propagation = Propagation.REQUIRED)
    @PostPersist
    @PostUpdate
    public void savePageSnapshot(Page page) {
        // Chốt chặn an toàn: Nếu page chưa có ID hợp lệ thì hủy bỏ ngay
        if (page.getId() == null) {
            return;
        }

        PageVersion newVersion = new PageVersion();
        
        // BÍ QUYẾT Ở ĐÂY: Dùng getReference() để nhờ Hibernate tạo ra một Proxy đại diện 
        // cho row có id = page.getId() đang nằm trong Persistence Context.
        // Cách này giúp vượt qua check Khóa ngoại mà không bị lỗi Transient instance!
        Page managedPageProxy = entityManager.getReference(Page.class, page.getId());
        newVersion.setPage(managedPageProxy);
        
        newVersion.setImageUrl(page.getImageUrl());
        newVersion.setCreatedAt(LocalDateTime.now());

        try {
            int currentVersionsCount = pageVersionRepository.countByPageId(page.getId());
            newVersion.setVersionNumber(currentVersionsCount + 1);
        } catch (Exception e) {
            newVersion.setVersionNumber(1);
        }

        pageVersionRepository.save(newVersion);
        
        System.out.println(">>> [VERSIONING LISTENER] Successfully backed up snapshot Version " 
                + newVersion.getVersionNumber() + " for Page ID: " + page.getId());
    }
}*/