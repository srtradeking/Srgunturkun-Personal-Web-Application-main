package com.webapp.backend.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.webapp.backend.dto.VideoMetadataDTO;
import com.webapp.backend.service.FileUploadService;
import com.webapp.backend.repository.VideoMetadataRepository;
import com.webapp.backend.mapper.VideoMetadataMapper;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Video Upload Controller
 * REST API endpoints for video management
 * Authentication required: JWT token with authority
 */
@Slf4j
@RestController
@RequestMapping("/videos")
@CrossOrigin(origins = "*", maxAge = 3600)
public class VideoUploadController {

    private final FileUploadService fileUploadService;
    private final VideoMetadataRepository videoMetadataRepository;
    private final VideoMetadataMapper videoMetadataMapper;

    public VideoUploadController(FileUploadService fileUploadService,
                               VideoMetadataRepository videoMetadataRepository,
                               VideoMetadataMapper videoMetadataMapper) {
        this.fileUploadService = fileUploadService;
        this.videoMetadataRepository = videoMetadataRepository;
        this.videoMetadataMapper = videoMetadataMapper;
    }

    /**
     * Upload single video
     * POST /api/videos/upload?postId=1
     * Authentication: JWT token required
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("postId") Long postId) {
        try {
            log.info("📹 Video upload request: postId={}, filename={}, size={}MB",
                    postId, file.getOriginalFilename(), file.getSize() / 1024 / 1024);

            VideoMetadataDTO videoMetadata = fileUploadService.uploadVideo(file, postId);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new UploadResponse(
                            "success",
                            "Video uploaded successfully",
                            videoMetadata
                    ));

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Invalid file: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("error", "Invalid file: " + e.getMessage()));

        } catch (IOException e) {
            log.error("❌ Upload failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("error", "Upload failed: " + e.getMessage()));

        } catch (Exception e) {
            log.error("❌ Unexpected error during upload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("error", "Unexpected error: " + e.getMessage()));
        }
    }

    /**
     * Upload multiple videos
     * POST /api/videos/upload-multiple?postId=1
     * Authentication: JWT token required
     */
    @PostMapping("/upload-multiple")
    public ResponseEntity<?> uploadMultipleVideos(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("postId") Long postId) {
        try {
            log.info("📹 Batch video upload request: postId={}, count={}", postId, files.length);

            List<VideoMetadataDTO> results = new java.util.ArrayList<>();
            List<String> errors = new java.util.ArrayList<>();

            for (MultipartFile file : files) {
                try {
                    VideoMetadataDTO metadata = fileUploadService.uploadVideo(file, postId);
                    results.add(metadata);
                } catch (Exception e) {
                    errors.add(String.format("%s: %s", file.getOriginalFilename(), e.getMessage()));
                }
            }

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new BatchUploadResponse(
                            errors.isEmpty() ? "success" : "partial",
                            String.format("Uploaded %d of %d files", results.size(), files.length),
                            results,
                            errors
                    ));

        } catch (Exception e) {
            log.error("❌ Batch upload failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("error", "Batch upload failed: " + e.getMessage()));
        }
    }

    /**
     * Get all videos for a post
     * GET /api/videos/posts/1
     * Authentication: JWT token required
     */
    @GetMapping("/posts/{postId}")
    public ResponseEntity<?> getVideosByPost(@PathVariable Long postId) {
        try {
            log.info("📺 Fetching videos for post: {}", postId);

            List<VideoMetadataDTO> videos = videoMetadataMapper.toDtoList(
                    videoMetadataRepository.findByPostIdAndIsDeletedFalse(postId)
                            .map(List::of)
                            .orElse(List.of())
            );

            return ResponseEntity.ok(new ListResponse(
                    "success",
                    String.format("Found %d videos", videos.size()),
                    videos
            ));

        } catch (Exception e) {
            log.error("❌ Failed to fetch videos for post: {}", postId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("error", "Failed to fetch videos"));
        }
    }

    /**
     * Get video metadata by ID
     * GET /api/videos/1
     * Authentication: JWT token required
     */
    @GetMapping("/{videoId}")
    public ResponseEntity<?> getVideoMetadata(@PathVariable Long videoId) {
        try {
            log.info("📺 Fetching video metadata: {}", videoId);

            Optional<ResponseEntity<?>> response = videoMetadataRepository.findById(videoId)
                    .map(video -> (ResponseEntity<?>) ResponseEntity.ok(new DataResponse<>(
                            "success",
                            "Video found",
                            videoMetadataMapper.toDto(video)
                    )));

            return response.orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("error", "Video not found")));

        } catch (Exception e) {
            log.error("❌ Failed to fetch video: {}", videoId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("error", "Failed to fetch video"));
        }
    }

    /**
     * Delete video
     * DELETE /api/videos/1
     * Authentication: JWT token required
     */
    @DeleteMapping("/{videoId}")
    public ResponseEntity<?> deleteVideo(@PathVariable Long videoId) {
        try {
            log.info("🗑️ Deleting video: {}", videoId);

            fileUploadService.deleteVideo(videoId);

            return ResponseEntity.ok(new SuccessResponse(
                    "success",
                    "Video deleted successfully"
            ));

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Video not found: {}", videoId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("error", "Video not found"));

        } 
          catch (Exception e) {
            log.error("❌ Unexpected error deleting video: {}", videoId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("error", "Failed to delete video"));
        }
    }

    /**
     * Get all active videos (non-deleted)
     * GET /api/videos/active/all
     * Authentication: JWT token required
     */
    @GetMapping("/active/all")
    public ResponseEntity<?> getAllActiveVideos() {
        try {
            log.info("📺 Fetching all active videos");

            List<VideoMetadataDTO> videos = videoMetadataMapper.toDtoList(
                    videoMetadataRepository.findAllActive()
            );

            return ResponseEntity.ok(new ListResponse(
                    "success",
                    String.format("Found %d active videos", videos.size()),
                    videos
            ));

        } catch (Exception e) {
            log.error("❌ Failed to fetch active videos", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("error", "Failed to fetch videos"));
        }
    }

    // Response DTOs

    public static class UploadResponse {
        public String status;
        public String message;
        public VideoMetadataDTO data;

        public UploadResponse(String status, String message, VideoMetadataDTO data) {
            this.status = status;
            this.message = message;
            this.data = data;
        }
    }

    public static class BatchUploadResponse {
        public String status;
        public String message;
        public List<VideoMetadataDTO> data;
        public List<String> errors;

        public BatchUploadResponse(String status, String message, List<VideoMetadataDTO> data, List<String> errors) {
            this.status = status;
            this.message = message;
            this.data = data;
            this.errors = errors;
        }
    }

    public static class ListResponse {
        public String status;
        public String message;
        public List<VideoMetadataDTO> data;

        public ListResponse(String status, String message, List<VideoMetadataDTO> data) {
            this.status = status;
            this.message = message;
            this.data = data;
        }
    }

    public static class DataResponse<T> {
        public String status;
        public String message;
        public T data;

        public DataResponse(String status, String message, T data) {
            this.status = status;
            this.message = message;
            this.data = data;
        }
    }

    public static class SuccessResponse {
        public String status;
        public String message;

        public SuccessResponse(String status, String message) {
            this.status = status;
            this.message = message;
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
