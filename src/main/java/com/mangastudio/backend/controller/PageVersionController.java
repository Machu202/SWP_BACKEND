package com.mangastudio.backend.controller;

import com.mangastudio.backend.entity.Page;
import com.mangastudio.backend.entity.PageVersion;
import com.mangastudio.backend.repository.PageRepository;
import com.mangastudio.backend.repository.PageVersionRepository;
import com.mangastudio.backend.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/page-versions")
@RequiredArgsConstructor
public class PageVersionController {

    private final PageVersionRepository pageVersionRepository;
    private final PageRepository pageRepository;

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
            throw new RuntimeException("Error: You do not have permission to restore this page version");
        }

        page.setImageUrl(version.getImageUrl());
        Page restoredPage = pageRepository.saveAndFlush(page);

        int nextVersionNumber = pageVersionRepository.findTopByPageIdOrderByVersionNumberDesc(page.getId())
                .map(PageVersion::getVersionNumber)
                .map(number -> number + 1)
                .orElse(1);

        PageVersion restoreSnapshot = PageVersion.builder()
                .page(pageRepository.getReferenceById(page.getId()))
                .imageUrl(version.getImageUrl())
                .versionNumber(nextVersionNumber)
                .createdAt(LocalDateTime.now())
                .build();
        pageVersionRepository.saveAndFlush(restoreSnapshot);

        return ResponseEntity.ok(restoredPage);
    }
}
