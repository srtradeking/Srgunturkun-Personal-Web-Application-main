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
public class NotificationsDTO {
    private Long id;
    private Long userId;
    private Long userProfileId;
    private Long postId;
    private Long commentId;
    private String type;
    private String title;
    private String content;
    private String metadata;
    private Boolean isRead;
    private Boolean isDeleted;
    private Boolean isHidden;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}