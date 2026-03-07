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
public class ConsentAuditDTO {
    private Long id;
    private String userId;
    private String consentType;
    private String consentStatus;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createdAt;
}