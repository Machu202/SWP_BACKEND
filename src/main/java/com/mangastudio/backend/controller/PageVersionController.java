package com.mangastudio.backend.controller;

import com.mangastudio.backend.entity.Page;
import com.mangastudio.backend.entity.PageVersion;
import com.mangastudio.backend.repository.PageRepository;
import com.mangastudio.backend.repository.PageVersionRepository;
import com.mangastudio.backend.repository.HitboxRepository;
import com.mangastudio.backend.entity.Hitbox;
import com.mangastudio.backend.security.UserDetailsImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Objects;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/v1/page-versions")
public class PageVersionController {

    private final PageVersionRepository pageVersionRepository;
    private final PageRepository pageRepository;
    private final HitboxRepository hitboxRepository;

    public PageVersionController(PageVersionRepository pageVersionRepository,
                                 PageRepository pageRepository,
                                 HitboxRepository hitboxRepository) {
        this.pageVersionRepository = pageVersionRepository;
        this.pageRepository = pageRepository;
        this.hitboxRepository = hitboxRepository;
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
                    dto.put("hitboxCount", hitboxRepository.findByPageVersionId(version.getId()).size());
                    dto.put("hitbox_count", hitboxRepository.findByPageVersionId(version.getId()).size());
                    return dto;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }


    @GetMapping("/{versionId}/hitboxes")
    public ResponseEntity<List<Map<String, Object>>> getVersionHitboxes(@PathVariable Long versionId) {
        pageVersionRepository.findById(versionId)
                .orElseThrow(() -> new RuntimeException("Error: Page version not found"));
        List<Map<String, Object>> response = hitboxRepository.findByPageVersionId(versionId).stream()
                .map(hitbox -> {
                    Map<String, Object> dto = new LinkedHashMap<>();
                    dto.put("id", hitbox.getId());
                    dto.put("xCoord", hitbox.getXCoord());
                    dto.put("x_coord", hitbox.getXCoord());
                    dto.put("yCoord", hitbox.getYCoord());
                    dto.put("y_coord", hitbox.getYCoord());
                    dto.put("width", hitbox.getWidth());
                    dto.put("height", hitbox.getHeight());
                    return dto;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{versionId}/restore")
    @Transactional
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

        String currentImageUrl = page.getImageUrl();
        List<Hitbox> activeHitboxes = new ArrayList<>(
                hitboxRepository.findByPageIdAndPageVersionIsNull(page.getId()));

        // Preserve hitboxes belonging to the image that is currently live. In
        // normal data that image already has a PageVersion. Restored clones
        // that are already represented by that version are removed instead of
        // being duplicated inside the historical snapshot.
        boolean switchingImages = !Objects.equals(currentImageUrl, version.getImageUrl());
        PageVersion currentVersion = currentImageUrl == null ? null : pageVersionRepository
                .findTopByPageIdAndImageUrlOrderByVersionNumberDesc(page.getId(), currentImageUrl)
                .orElse(null);
        if (switchingImages && currentVersion != null && !activeHitboxes.isEmpty()) {
            List<Hitbox> alreadyArchived = hitboxRepository.findByPageVersionId(currentVersion.getId());
            List<Hitbox> toArchive = new ArrayList<>();
            List<Hitbox> duplicateRestoredClones = new ArrayList<>();
            for (Hitbox active : activeHitboxes) {
                boolean represented = active.getTask() == null && alreadyArchived.stream()
                        .anyMatch(saved -> sameGeometry(active, saved));
                if (represented) {
                    duplicateRestoredClones.add(active);
                } else {
                    active.setPageVersion(currentVersion);
                    toArchive.add(active);
                }
            }
            if (!toArchive.isEmpty()) hitboxRepository.saveAll(toArchive);
            if (!duplicateRestoredClones.isEmpty()) hitboxRepository.deleteAll(duplicateRestoredClones);
        }

        page.setImageUrl(version.getImageUrl());
        Page restoredPage = pageRepository.saveAndFlush(page);

        // The archived hitboxes remain attached to the historical version.
        // Clone only their geometry into the live page so Restore immediately
        // recreates the correct editable overlay without removing history.
        List<Hitbox> archivedHitboxes = hitboxRepository.findByPageVersionId(versionId);
        List<Hitbox> currentActive = hitboxRepository.findByPageIdAndPageVersionIsNull(page.getId());
        List<Hitbox> restoredActive = archivedHitboxes.stream()
                .filter(saved -> currentActive.stream().noneMatch(active -> sameGeometry(active, saved)))
                .map(saved -> Hitbox.builder()
                        .page(restoredPage)
                        .pageVersion(null)
                        .createdBy(saved.getCreatedBy())
                        .xCoord(saved.getXCoord())
                        .yCoord(saved.getYCoord())
                        .width(saved.getWidth())
                        .height(saved.getHeight())
                        .build())
                .collect(Collectors.toList());
        if (!restoredActive.isEmpty()) hitboxRepository.saveAll(restoredActive);

        // Restoring selects an existing snapshot; it never creates another
        // PageVersion record.
        return ResponseEntity.ok(restoredPage);
    }

    private boolean sameGeometry(Hitbox left, Hitbox right) {
        return sameNumber(left.getXCoord(), right.getXCoord())
                && sameNumber(left.getYCoord(), right.getYCoord())
                && sameNumber(left.getWidth(), right.getWidth())
                && sameNumber(left.getHeight(), right.getHeight());
    }

    private boolean sameNumber(Double left, Double right) {
        if (left == null || right == null) return left == right;
        return Math.abs(left - right) < 0.0001d;
    }
}

