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
public class VideoMetadataDTO {
    private Long id;
    private Long postId;
    private String videoUrl;
    private String originalFilename;
    private Long fileSizeBytes;
    private Integer durationSeconds;
    private String videoCodec;
    private Integer width;
    private Integer height;
    private Double frameRate;
    private String mimeType;
    private String thumbnailUrl;
    private String storageType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isDeleted;
    private LocalDateTime deletedAt;

}