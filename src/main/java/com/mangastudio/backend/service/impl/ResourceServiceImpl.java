package com.mangastudio.backend.service.impl;

import com.mangastudio.backend.entity.Resource;
import com.mangastudio.backend.entity.User;
import com.mangastudio.backend.repository.ResourceRepository;
import com.mangastudio.backend.repository.UserRepository;
import com.mangastudio.backend.service.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {

    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;
    private final String UPLOAD_DIR = "uploads";

    @Override
    @Transactional
    public Resource uploadFile(MultipartFile file, Long uploaderId, String resourceType) {
        User uploader = userRepository.findById(uploaderId)
                .orElseThrow(() -> new RuntimeException("Error: User not found"));

        try {
            // 1. Create upload directory if it does not exist
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 2. Generate a unique file name to prevent overwriting
            String originalFileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
            String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

            // 3. Save the file to the local file system
            Path filePath = uploadPath.resolve(uniqueFileName);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            }

            // 4. Generate the URL to access the file (Assuming backend runs on localhost:8080)
            String fileUrl = "http://localhost:8080/uploads/" + uniqueFileName;

            // 5. Save the record in the Database
            Resource resource = Resource.builder()
                    .resourceType(resourceType)
                    .fileUrl(fileUrl)
                    .uploadedBy(uploader)
                    .build();

            return resourceRepository.save(resource);

        } catch (IOException ex) {
            throw new RuntimeException("Could not store file. Please try again!", ex);
        }
    }
}