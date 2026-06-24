package com.mangastudio.backend.service;

import com.mangastudio.backend.entity.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface ResourceService {
    Resource uploadFile(MultipartFile file, Long uploaderId, String resourceType);
}