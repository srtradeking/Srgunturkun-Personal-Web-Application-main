package com.webapp.backend.controller;

import com.webapp.backend.service.FileUploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Image Upload Controller
 * Handles generic image uploads (for posts, profiles, etc.)
 * Authentication required: JWT token
 */
@Slf4j
@RestController
@RequestMapping("/images")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ImageUploadController {

    private final FileUploadService fileUploadService;

    public ImageUploadController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    /**
     * Upload an image
     * POST /api/images/upload
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            log.info("📸 Image upload request: filename={}, size={}KB", 
                    file.getOriginalFilename(), file.getSize() / 1024);

            String imageUrl = fileUploadService.uploadImage(file);

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Image uploaded successfully");
            response.put("url", imageUrl);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Invalid file: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("error", e.getMessage()));

        } catch (IOException e) {
            log.error("❌ Upload failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("error", "Upload failed: " + e.getMessage()));

        } catch (Exception e) {
            log.error("❌ Unexpected error during upload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("error", "Unexpected error: " + e.getMessage()));
        }
    }

    public static class ErrorResponse {
        public String status;
        public String message;

        public ErrorResponse(String status, String message) {
            this.status = status;
            this.message = message;
        }
    }
}
