package com.mangastudio.backend.listener;

import com.mangastudio.backend.entity.Page;
import com.mangastudio.backend.entity.PageVersion;
import com.mangastudio.backend.repository.PageVersionRepository;
import jakarta.persistence.PreUpdate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor // <--- 1. Gắn bùa Lombok
public class PageVersioningListener {

    // 2. Chuyển thành private final, TIỄN @Autowired VÀO DĨ VÃNG
    private final ObjectProvider<PageVersionRepository> versionRepoProvider;

    @PreUpdate
    public void captureSnapshotBeforeUpdate(Page page) {
        versionRepoProvider.ifAvailable(repo -> {
            PageVersion snapshot = PageVersion.builder()
                    .pageId(page.getPageId())
                    .versionTag("v-auto-" + UUID.randomUUID().toString().substring(0, 5))
                    .commitMessage("System auto-backup before overwrite")
                    .oldImageUrl(page.getImageUrl())
                    .createdAt(LocalDateTime.now())
                    .build();
            repo.save(snapshot);
        });
    }
}