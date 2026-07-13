package com.mangastudio.backend.controller;

import com.mangastudio.backend.entity.Page;
import com.mangastudio.backend.entity.PageVersion;
import com.mangastudio.backend.repository.PageRepository;
import com.mangastudio.backend.repository.PageVersionRepository;
import com.mangastudio.backend.security.UserDetailsImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;

@RestController
@RequestMapping("/api/v1/page-versions")
public class PageVersionController {

    private final PageVersionRepository pageVersionRepository;
    private final PageRepository pageRepository;

    public PageVersionController(PageVersionRepository pageVersionRepository, PageRepository pageRepository) {
        this.pageVersionRepository = pageVersionRepository;
        this.pageRepository = pageRepository;
    }

    @GetMapping("/pages/{pageId}")
    public ResponseEntity<List<Map<String, Object>>> getVersionsByPage(@PathVariable Long pageId) {
        pageRepository.findById(pageId)
                .orElseThrow(() -> new RuntimeException("Error: Page not found"));
        List<Map<String, Object>> response = pageVersionRepository.findByPageIdOrderByCreatedAtDesc(pageId)
                .stream()
                .map(version -> {
                    Map<String, Object> dto = new LinkedHashMap<>();
                    dto.put("id", version.getId());
                    dto.put("pageId", pageId);
                    dto.put("page_id", pageId);
                    dto.put("imageUrl", version.getImageUrl());
                    dto.put("image_url", version.getImageUrl());
                    dto.put("versionNumber", version.getVersionNumber());
                    dto.put("version_number", version.getVersionNumber());
                    dto.put("createdAt", version.getCreatedAt());
                    dto.put("created_at", version.getCreatedAt());
                    return dto;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{versionId}/restore")
    public ResponseEntity<Page> restoreVersion(
            @PathVariable Long versionId,
            Authentication authentication) {

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        PageVersion version = pageVersionRepository.findById(versionId)
                .orElseThrow(() -> new RuntimeException("Error: Page version not found"));

        Page page = version.getPage();
        if (page == null || page.getId() == null) {
            throw new RuntimeException("Error: Page for this version no longer exists");
        }

        if (!page.getChapter().getMangaSeries().getMangaka().getId().equals(userDetails.getId())) {
            throw new AccessDeniedException("You do not have permission to restore this page version");
        }

        page.setImageUrl(version.getImageUrl());
        Page restoredPage = pageRepository.saveAndFlush(page);
        // Restoring means selecting an existing historical snapshot as the
        // current page image. Do not clone it into a new version; otherwise
        // restoring V1 incorrectly creates V2/V3 and the history becomes noisy.
        return ResponseEntity.ok(restoredPage);
    }
}
