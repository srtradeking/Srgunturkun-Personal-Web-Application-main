package com.webapp.backend.controller;

import com.webapp.backend.dto.SignedUploadRequest;
import com.webapp.backend.dto.SignedUploadResponse;
import com.webapp.backend.repository.UserProfileRepository;
import com.webapp.backend.service.LocalFileStorageService;
import com.webapp.backend.model.UserProfile;
import com.webapp.backend.security.FileInclusionValidator;
import com.webapp.backend.security.InputSanitizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import java.io.InputStream;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.util.StreamUtils;

@Slf4j
@RestController
@RequestMapping("/storage")
@RequiredArgsConstructor
public class StorageController {

    private final LocalFileStorageService localFileStorageService;
    private final UserProfileRepository userProfileRepository;
    private final FileInclusionValidator fileInclusionValidator;
    private final InputSanitizationService sanitizationService;

    @PostMapping("/signed-upload")
    public ResponseEntity<SignedUploadResponse> getSignedUploadUrl(
            @RequestBody SignedUploadRequest request,
            Authentication authentication) {

        String principal = (String) authentication.getPrincipal();

        Long userId = null;
        Optional<UserProfile> profileOpt = userProfileRepository.findByUsername(principal);
        if (profileOpt.isPresent()) {
            userId = profileOpt.get().getId();
        }

        // Validate and sanitize filename
        String fileName = request.getFileName();
        if (fileName != null && !fileName.isEmpty()) {
            fileName = sanitizationService.sanitizeFilename(fileName);
        }

        String userPart = userId != null ? String.valueOf(userId) : principal;
        String ext = extractExtension(request.getMimeType(), fileName);
        
        // Validate extension
        sanitizationService.validateNoCommandInjection(ext);
        
        String objectKey = String.format(
                "media/%s/%d-%s.%s",
                userPart,
                System.currentTimeMillis(),
                UUID.randomUUID(),
                ext
        );
        
        // Validate the generated object key
        fileInclusionValidator.validateResourceKey(objectKey);

        String mimeType = request.getMimeType();
        
        // Generate upload URL for local storage
        String uploadUrl = null; // Local storage doesn't need signed URLs
        
        if (uploadUrl == null) {
            // For local storage, return the object key so frontend can upload directly
            uploadUrl = "/api/storage/upload/" + objectKey;
        }
        
        // For public access, we point to our own serveMediaFile endpoint,
        // which will redirect to the signed Worker URL.
        // This keeps the database URLs stable/clean while using the Worker for delivery.
        // Public URL generation moved to frontend logic or handled via /api/storage/ proxies

        SignedUploadResponse response = new SignedUploadResponse();
        response.setUploadUrl(uploadUrl);
        response.setObjectKey(objectKey);
        
        log.info("Generated local upload URL for user={}, key={}", userPart, objectKey);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/upload/{objectKey}")
    public ResponseEntity<String> uploadFile(
            @PathVariable String objectKey,
            @RequestParam("file") MultipartFile file) {
        
        log.info("Uploading file to local storage: {}", objectKey);
        
        try {
            // Validate the object key to prevent path traversal
            fileInclusionValidator.validateResourceKey(objectKey);
            
            // Upload file to local storage
            String filename = localFileStorageService.uploadFile(file, objectKey);
            
            return ResponseEntity.ok("File uploaded successfully: " + filename);
        } catch (Exception e) {
            log.error("Failed to upload file: {}", objectKey, e);
            return ResponseEntity.internalServerError().body("Upload failed: " + e.getMessage());
        }
    }

    @GetMapping("/{filename}")
    public ResponseEntity<Void> serveRootFile(@PathVariable String filename) {
        log.info("Request to serve root file: {}", filename);
        
        // Validate key
        fileInclusionValidator.validateResourceKey(filename);
        
        // For local storage, serve file directly
        try {
            InputStream stream = localFileStorageService.downloadFile(filename);
            if (stream == null) {
                return ResponseEntity.notFound().build();
            }
            
            byte[] bytes = StreamUtils.copyToByteArray(stream);
            String contentType = "application/octet-stream";
            if (filename.toLowerCase().endsWith(".jpg") || filename.toLowerCase().endsWith(".jpeg")) contentType = "image/jpeg";
            else if (filename.toLowerCase().endsWith(".png")) contentType = "image/png";
            else if (filename.toLowerCase().endsWith(".gif")) contentType = "image/gif";
            else if (filename.toLowerCase().endsWith(".mp4")) contentType = "video/mp4";
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .contentLength(bytes.length)
                    .body(new ByteArrayResource(bytes));
        } catch (Exception e) {
            log.error("Error serving file: {}", filename, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/media/{userPart}/{filename}")
    public ResponseEntity<Resource> serveMediaFile(
            @PathVariable String userPart,
            @PathVariable String filename) {
        
        String objectKey = "media/" + userPart + "/" + filename;
        log.info("Request to serve media file: {}", objectKey);
        
        // Validate key to prevent traversal
        fileInclusionValidator.validateResourceKey(objectKey);
        
        // Try to serve from local storage
        try {
            InputStream stream = localFileStorageService.downloadFile(objectKey);
            if (stream == null) {
                log.warn("File not found in local storage: {}", objectKey);
                return ResponseEntity.notFound().build();
            }
            
            byte[] bytes = StreamUtils.copyToByteArray(stream);
            log.info("Successfully retrieved {} bytes for key: {}", bytes.length, objectKey);
            
            String contentType = "application/octet-stream";
            if (filename.toLowerCase().endsWith(".jpg") || filename.toLowerCase().endsWith(".jpeg")) contentType = "image/jpeg";
            else if (filename.toLowerCase().endsWith(".png")) contentType = "image/png";
            else if (filename.toLowerCase().endsWith(".gif")) contentType = "image/gif";
            else if (filename.toLowerCase().endsWith(".mp4")) contentType = "video/mp4";
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .contentLength(bytes.length)
                    .body(new ByteArrayResource(bytes));

        } catch (Exception e) {
            log.error("Error serving media file: {}", objectKey, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private String extractExtension(String mimeType, String fileNameFallback) {
        if (mimeType != null && mimeType.contains("/")) {
            return mimeType.substring(mimeType.indexOf('/') + 1);
        }
        if (fileNameFallback != null && fileNameFallback.contains(".")) {
            return fileNameFallback.substring(fileNameFallback.lastIndexOf('.') + 1);
        }
        return "bin";
    }
}
