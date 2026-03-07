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
public class UserProfilesDTO {
    private Long id;
    private Long userId;
    private String username;
    private String email;
    private String displayName;
    private String bio;
    private String profilePictureUrl;
    private Boolean isActive;
    private Boolean isVerified;
    private Boolean isBanned;
    private String banReason;
    private LocalDateTime bannedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;
    private Long totalLikesPoint;

}