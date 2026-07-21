package com.mangastudio.backend.service;

import com.mangastudio.backend.entity.Resource;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface ResourceService {
    Resource uploadFile(MultipartFile file, Long uploaderId, String resourceType);
    
    // [FE-45] Resource-library operations.
    List<Resource> getAllResources();
    void deleteResource(Long resourceId, Long currentUserId);
}
