package com.webapp.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "video_metadata")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "video_url", nullable = false, length = 2048)
    private String videoUrl;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "video_codec", length = 50)
    private String videoCodec;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "frame_rate")
    private Double frameRate;

    @Column(name = "mime_type", length = 50)
    private String mimeType;

    @Column(name = "thumbnail_url", length = 2048)
    private String thumbnailUrl;

    @Column(name = "storage_type", length = 20)
    @Builder.Default
    private String storageType = "local";

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

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
