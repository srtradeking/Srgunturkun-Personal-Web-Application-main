package com.webapp.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "username", nullable = false, length = 255, unique = true)
    private String username;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "display_name", length = 255)
    private String displayName;

    @Column(name = "bio", columnDefinition = "text")
    private String bio;

    @Column(name = "profile_picture_url", length = 1024)
    private String profilePictureUrl;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private Boolean isVerified = false;

    @Column(name = "is_banned", nullable = false)
    @Builder.Default
    private Boolean isBanned = false;

    @Column(name = "ban_reason", columnDefinition = "text")
    private String banReason;

    @Column(name = "banned_at")
    private LocalDateTime bannedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "total_likes_point", nullable = false)
    @Builder.Default
    private Long totalLikesPoint = 0L;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
