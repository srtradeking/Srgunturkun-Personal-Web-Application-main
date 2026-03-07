package com.webapp.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.webapp.backend.dto.VideoMetadataDTO;
import com.webapp.backend.model.VideoMetadata;
import com.webapp.backend.repository.VideoMetadataRepository;
import com.webapp.backend.mapper.VideoMetadataMapper;
import com.webapp.backend.service.LocalFileStorageService;
import com.webapp.backend.security.InputSanitizationService;
import com.webapp.backend.security.FilePathValidator;
import com.webapp.backend.security.FileUploadValidator;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * File Upload Service
 * Handles video file uploads and storage
 * Supports: Local File Storage
 */
@Slf4j
@Service
public class FileUploadService {

    private final VideoMetadataRepository videoMetadataRepository;
    private final VideoMetadataMapper videoMetadataMapper;
    private final LocalFileStorageService localFileStorageService;
    private final InputSanitizationService sanitizationService;
    private final FilePathValidator filePathValidator;
    private final FileUploadValidator fileUploadValidator;

    @Value("${file.upload.max-file-size}")
    private Long maxFileSize;

    @Value("${file.upload.allowed-types:mp4,webm,mov,avi,mkv}")
    private String allowedVideoTypes;

    @Value("${file.upload.allowed-image-types:jpg,jpeg,png,gif,webp}")
    private String allowedImageTypes;

    public FileUploadService(VideoMetadataRepository videoMetadataRepository,
                           VideoMetadataMapper videoMetadataMapper,
                           LocalFileStorageService localFileStorageService,
                           InputSanitizationService sanitizationService,
                           FilePathValidator filePathValidator,
                           FileUploadValidator fileUploadValidator) {
        this.videoMetadataRepository = videoMetadataRepository;
        this.videoMetadataMapper = videoMetadataMapper;
        this.localFileStorageService = localFileStorageService;
        this.sanitizationService = sanitizationService;
        this.filePathValidator = filePathValidator;
        this.fileUploadValidator = fileUploadValidator;
    }

    /**
     * Upload video file and save metadata
     */
    public VideoMetadataDTO uploadVideo(MultipartFile file, Long postId) throws IOException {
        log.info("📹 Uploading video for post: {}", postId);

        // Comprehensive upload validation
        fileUploadValidator.validateUpload(file, FileUploadValidator.FileType.VIDEO);

        // Validate and sanitize filename
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }
        
        // Validate filename for security threats
        filePathValidator.validateFileExtension(originalFilename, FilePathValidator.FileType.VIDEO);
        
        // Sanitize filename
        String sanitizedFilename = sanitizationService.sanitizeFilename(originalFilename);
        log.debug("Sanitized filename: {} -> {}", originalFilename, sanitizedFilename);

        // Validate file
        validateFile(file, allowedVideoTypes);

        // Generate unique filename using sanitized name
        String uniqueFilename = generateUniqueFilename(sanitizedFilename);
        
        // Upload to local storage
        String key = localFileStorageService.uploadFile(file, uniqueFilename);
        // Use /api/storage/ to ensure correct proxy routing relative to domain root
        String fileUrl = "/api/storage/" + key;

        // Create and save video metadata
        VideoMetadata videoMetadata = VideoMetadata.builder()
                .post(null) // Will be set later when post is created
                .videoUrl(fileUrl)
                .originalFilename(sanitizedFilename)
                .fileSizeBytes(file.getSize())
                .mimeType(file.getContentType())
                .storageType("local")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isDeleted(false)
                .build();

        VideoMetadata saved = videoMetadataRepository.save(videoMetadata);
        log.info("✅ Video uploaded successfully: {} ({}MB)", fileUrl, file.getSize() / 1024 / 1024);

        return videoMetadataMapper.toDto(saved);
    }

    /**
     * Upload image file and return URL
     */
    public String uploadImage(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        log.info("📸 Uploading image: {} ({}KB)", originalFilename, file.getSize() / 1024);

        // Comprehensive upload validation
        fileUploadValidator.validateUpload(file, FileUploadValidator.FileType.IMAGE);

        // Validate and sanitize filename
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }
        
        // Validate filename for security threats
        filePathValidator.validateFileExtension(originalFilename, FilePathValidator.FileType.IMAGE);
        
        // Sanitize filename
        String sanitizedFilename = sanitizationService.sanitizeFilename(originalFilename);
        log.debug("Sanitized filename: {} -> {}", originalFilename, sanitizedFilename);

        // Validate file
        validateFile(file, allowedImageTypes);

        // Generate unique filename using sanitized name
        String uniqueFilename = generateUniqueFilename(sanitizedFilename);

        // Upload to local storage
        String key = localFileStorageService.uploadFile(file, uniqueFilename);
        String fileUrl = "/api/storage/" + key;
        
        log.info("✅ Image uploaded successfully: {}", fileUrl);
        return fileUrl;
    }

    /**
     * Validate file before upload
     */
    private void validateFile(MultipartFile file, String allowedTypes) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException(
                String.format("File size (%d) exceeds maximum allowed (%d)", 
                    file.getSize(), maxFileSize)
            );
        }

        String[] allowedExtensions = allowedTypes.split(",");
        String filename = file.getOriginalFilename();
        String fileExtension = getFileExtension(filename).toLowerCase();

        boolean isAllowed = false;
        for (String ext : allowedExtensions) {
            if (ext.trim().equalsIgnoreCase(fileExtension)) {
                isAllowed = true;
                break;
            }
        }

        if (!isAllowed) {
            throw new IllegalArgumentException(
                String.format("File type '%s' is not allowed. Allowed types: %s", 
                    fileExtension, allowedTypes)
            );
        }

        log.debug("✅ File validation passed: {} ({}MB)", filename, file.getSize() / 1024 / 1024);
    }

    /**
     * Delete video from local storage and soft-delete metadata
     */
    public void deleteVideo(Long videoId) {
        log.info("🗑️ Request to delete video with ID: {}", videoId);

        // Fetch metadata
        VideoMetadata metadata = videoMetadataRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video not found: " + videoId));

        if (metadata.getIsDeleted()) {
            log.warn("⚠️ Video already deleted: {}", videoId);
            return;
        }

        // Extract filename from URL (R2 stores by key name)
        String videoUrl = metadata.getVideoUrl();
        String key = extractKeyFromUrl(videoUrl);

        try {
            // Delete file from local storage
            localFileStorageService.deleteFile(key);
            log.info("🗑️ Successfully deleted video from local storage: {}", key);
        } catch (Exception e) {
            log.error("❌ Failed to delete video from local storage: {}", key, e);
            throw new RuntimeException("Could not delete video from local storage", e);
        }

        // Soft delete metadata
        metadata.setIsDeleted(true);
        metadata.setUpdatedAt(LocalDateTime.now());
        videoMetadataRepository.save(metadata);

        log.info("✅ Video metadata soft-deleted: {}", videoId);
    }

    /**
     * Generate unique filename
     */
    private String generateUniqueFilename(String originalFilename) {
        String fileExtension = getFileExtension(originalFilename);
        String uniqueName = UUID.randomUUID().toString();
        return uniqueName + "." + fileExtension;
    }

    /**
     * Extract file extension
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    /**
     * Extract object key from file URL
     */
    private String extractKeyFromUrl(String fileUrl) {
        if (fileUrl == null || !fileUrl.contains("/")) {
            throw new IllegalArgumentException("Invalid R2 file URL: " + fileUrl);
        }

        // Validate URL doesn't contain path traversal
        sanitizationService.validateNoPathTraversal(fileUrl);
        sanitizationService.validateNoCommandInjection(fileUrl);

        // Get everything after the last '/'
        String key = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
        
        // Additional validation on the extracted key
        filePathValidator.validateFilename(key);
        
        return key;
    }
}
