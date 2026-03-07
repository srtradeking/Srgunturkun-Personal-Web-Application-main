package com.webapp.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentsDTO {
    private Long id;
    private String userId;
    private Long userProfileId;
    private Long postId;
    private String content;
    private Integer likesCount;
    private Integer reportsCount;
    private Boolean isDeleted;
    private Boolean isHidden;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Long parentCommentId;
    private String ppUrl; // Profile picture URL
    private String userDisplayName; // Display name of the user
}