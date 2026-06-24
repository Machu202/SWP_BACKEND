package com.mangastudio.backend.controller;

import com.mangastudio.backend.entity.Resource;
import com.mangastudio.backend.security.UserDetailsImpl;
import com.mangastudio.backend.service.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    @PostMapping("/upload")
    public ResponseEntity<Resource> uploadFile(
            Authentication authentication,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "resourceType", defaultValue = "PAGE_IMAGE") String resourceType) {

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Long currentUserId = userDetails.getId();

        Resource savedResource = resourceService.uploadFile(file, currentUserId, resourceType);
        return ResponseEntity.ok(savedResource);
    }
}