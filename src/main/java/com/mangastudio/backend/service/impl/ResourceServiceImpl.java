package com.mangastudio.backend.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.mangastudio.backend.entity.Resource;
import com.mangastudio.backend.entity.User;
import com.mangastudio.backend.repository.ResourceRepository;
import com.mangastudio.backend.repository.UserRepository;
import com.mangastudio.backend.service.ResourceService;
import com.mangastudio.backend.service.UploadPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {

    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;
    private final Cloudinary cloudinary;

    @Autowired
    private UploadPolicyService uploadPolicyService;

    @Override
    @Transactional
    public Resource uploadFile(MultipartFile file, Long uploaderId, String resourceType) {
        User uploader = userRepository.findById(uploaderId)
                .orElseThrow(() -> new RuntimeException("Error: User not found"));
        uploadPolicyService.validateFile(file);

        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "resource_type", "auto",
                    "folder", "mangastudio_resources"
            ));

            String secureFileUrl = uploadResult.get("secure_url").toString();
            String publicId = uploadResult.get("public_id").toString(); // Cloudinary asset identifier

            Resource resource = Resource.builder()
                    .resourceType(resourceType)
                    .fileUrl(secureFileUrl)
                    .publicId(publicId) // Store the identifier in SQL.
                    .uploadedBy(uploader)
                    .build();

            return resourceRepository.save(resource);

        } catch (IOException ex) {
            throw new RuntimeException("Could not upload file to Cloudinary. Please try again!", ex);
        }
    }

    @Override
    public List<Resource> getAllResources() {
        return resourceRepository.findAll();
    }

    @Override
    @Transactional
    public void deleteResource(Long resourceId, Long currentUserId) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new RuntimeException("Error: Resource not found"));

        // Security check: only the uploader can delete this resource.
        if (!resource.getUploadedBy().getId().equals(currentUserId)) {
            throw new RuntimeException("Error: You do not have permission to delete this resource");
        }

        try {
            // 1. Delete the physical asset from Cloudinary.
            if (resource.getPublicId() != null) {
                cloudinary.uploader().destroy(resource.getPublicId(), ObjectUtils.emptyMap());
            }
            
            // 2. Delete the database record.
            resourceRepository.delete(resource);

        } catch (IOException ex) {
            throw new RuntimeException("Error: Failed to delete file from Cloudinary", ex);
        }
    }
}
