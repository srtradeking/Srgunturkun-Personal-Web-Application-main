package com.webapp.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "posts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_profile_id")
    private UserProfile userProfile;

    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "image_url", length = 2048)
    private String imageUrl;

    @Column(name = "video_url", length = 2048)
    private String videoUrl;

    @Column(name = "game", length = 255)
    private String game;

    @Column(name = "likes_count", nullable = false)
    @Builder.Default
    private Integer likesCount = 0;

    @Column(name = "comments_count", nullable = false)
    @Builder.Default
    private Integer commentsCount = 0;

    @Column(name = "reports_count", nullable = false)
    @Builder.Default
    private Integer reportsCount = 0;

    @Column(name = "is_published", nullable = false)
    @Builder.Default
    private Boolean isPublished = true;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "is_hidden", nullable = false)
    @Builder.Default
    private Boolean isHidden = false;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PostStatus status = PostStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // Add type field for distinguishing video vs image posts
    @Transient
    private String type;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
}
