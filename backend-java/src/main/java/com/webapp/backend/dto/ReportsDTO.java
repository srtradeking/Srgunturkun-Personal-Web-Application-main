package com.webapp.backend.dto;

import com.webapp.backend.model.Report;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportsDTO {
    private UUID id;
    private String type;
    private String category;
    private String contentId;
    private String userId;
    private String description;
    private String status;
    private LocalDateTime reportedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime resolvedAt;
    private String resolution;
    private String dismissalReason;
    private Boolean isActive;
    private Boolean hasAppealed;
    private String appealDescription;
    private LocalDateTime appealedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ReportsDTO fromEntity(Report report) {
        return ReportsDTO.builder()
                .id(report.getId())
                .type(report.getType())
                .category(report.getCategory())
                .contentId(report.getContentId())
                .userId(report.getUserId())
                .description(report.getDescription())
                .status(report.getStatus())
                .reportedAt(report.getReportedAt())
                .reviewedAt(report.getReviewedAt())
                .resolvedAt(report.getResolvedAt())
                .resolution(report.getResolution())
                .dismissalReason(report.getDismissalReason())
                .isActive(report.getIsActive())
                .hasAppealed(report.getHasAppealed())
                .appealDescription(report.getAppealDescription())
                .appealedAt(report.getAppealedAt())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }
}