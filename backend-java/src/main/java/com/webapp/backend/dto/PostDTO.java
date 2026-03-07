package com.webapp.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for Post entity
 * Maps to private.posts table in PostgreSQL
 * Includes denormalized user profile info for frontend convenience
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostDTO {
    // Core post fields from database
    private Long id;
    private Long userId;
    private Long userProfileId;
    private String title;
    private String content;
    private String imageUrl;
    private String videoUrl;
    private String game;
    private Integer likesCount;
    private Integer commentsCount;
    private Integer reportsCount;
    private Boolean isPublished;
    private Boolean isDeleted;
    private Boolean isHidden;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    
    // Computed/denormalized fields for client use (not persisted)
    private String type; // 'video' or 'image' - computed based on imageUrl/videoUrl
    private String url; // Media URL - computed from imageUrl or videoUrl
    private String mimeType; // MIME type for video/image
    
    // Denormalized user profile info for frontend display
    private String userDisplayName; // User's display name
    private String ppUrl; // User's profile picture URL
    private String description; // Alias for content (for frontend compatibility)
}

