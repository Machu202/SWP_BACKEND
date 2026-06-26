package com.mangastudio.backend.controller;

import com.mangastudio.backend.entity.Page;
import com.mangastudio.backend.security.UserDetailsImpl;
import com.mangastudio.backend.service.PageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pages")
@RequiredArgsConstructor
public class PageController {

    private final PageService pageService;

    @PostMapping("/chapter/{chapterId}")
    public ResponseEntity<Page> addPageToChapter(
            @PathVariable Long chapterId,
            @RequestParam("pageNumber") Integer pageNumber,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Page savedPage = pageService.addPageToChapter(chapterId, pageNumber, file, userDetails.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(savedPage);
    }

    // [FE-43] Điểm gọi API để ghi đè trang cũ bằng hình ảnh mới
    @PutMapping("/{id}/image")
    public ResponseEntity<Page> updatePageImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Page updatedPage = pageService.updatePageImage(id, file, userDetails.getId());
        return ResponseEntity.ok(updatedPage);
    }

    @GetMapping("/chapter/{chapterId}")
    public ResponseEntity<List<Page>> getPagesByChapter(@PathVariable Long chapterId) {
        return ResponseEntity.ok(pageService.getPagesByChapter(chapterId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletePage(
            @PathVariable Long id,
            Authentication authentication) {

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        pageService.deletePage(id, userDetails.getId());
        return ResponseEntity.ok("Page deleted successfully.");
    }
}