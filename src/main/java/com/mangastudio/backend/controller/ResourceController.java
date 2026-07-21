package com.mangastudio.backend.controller;

import com.mangastudio.backend.entity.Resource;
import com.mangastudio.backend.security.UserDetailsImpl;
import com.mangastudio.backend.service.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import java.util.List;

@RestController
@RequestMapping("/api/v1/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Resource> uploadFile(
            Authentication authentication,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "resourceType", defaultValue = "PAGE_IMAGE") String resourceType) {

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Resource savedResource = resourceService.uploadFile(file, userDetails.getId(), resourceType);
        return ResponseEntity.ok(savedResource);
    }

    // Returns every resource.
    @GetMapping
    public ResponseEntity<List<Resource>> getAllResources() {
        return ResponseEntity.ok(resourceService.getAllResources());
    }

    // Deletes a resource from both the database and cloud storage.
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteResource(
            @PathVariable Long id,
            Authentication authentication) {
        
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        resourceService.deleteResource(id, userDetails.getId());
        return ResponseEntity.ok("Resource deleted successfully from Server and Cloud.");
    }
}
