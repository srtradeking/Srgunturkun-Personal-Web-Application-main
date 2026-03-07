package com.webapp.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "type", nullable = false, length = 20)
    private String type;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "content_id", nullable = false, length = 255)
    private String contentId;

    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "reported_at", nullable = false)
    @Builder.Default
    private LocalDateTime reportedAt = LocalDateTime.now();

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolution", columnDefinition = "text")
    private String resolution;

    @Column(name = "dismissal_reason", columnDefinition = "text")
    private String dismissalReason;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "has_appealed", nullable = false)
    @Builder.Default
    private Boolean hasAppealed = false;

    @Column(name = "appeal_description", columnDefinition = "text")
    private String appealDescription;

    @Column(name = "appealed_at")
    private LocalDateTime appealedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        reportedAt = LocalDateTime.now();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
}
