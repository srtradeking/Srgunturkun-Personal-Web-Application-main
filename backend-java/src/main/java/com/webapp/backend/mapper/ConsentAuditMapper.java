package com.webapp.backend.mapper;

import com.webapp.backend.dto.ConsentAuditDTO;
import com.webapp.backend.model.ConsentAudit;
import org.springframework.stereotype.Component;

/**
 * Mapper for ConsentAudit entity to DTO conversion
 * Simplified to match new database schema (removed JSONB categories)
 */
@Component
public class ConsentAuditMapper {

    /**
     * Convert ConsentAudit entity to DTO
     */
    public ConsentAuditDTO toDTO(ConsentAudit consentAudit) {
        if (consentAudit == null) {
            return null;
        }

        return ConsentAuditDTO.builder()
                .id(consentAudit.getId())
                .userId(consentAudit.getUserId())
                .consentType(consentAudit.getConsentType())
                .consentStatus(consentAudit.getConsentStatus())
                .ipAddress(consentAudit.getIpAddress())
                .userAgent(consentAudit.getUserAgent())
                .createdAt(consentAudit.getCreatedAt())
                .build();
    }

    /**
     * Convert ConsentAuditDTO to entity
     */
    public ConsentAudit toEntity(ConsentAuditDTO consentAuditDTO) {
        if (consentAuditDTO == null) {
            return null;
        }

        return ConsentAudit.builder()
                .id(consentAuditDTO.getId())
                .userId(consentAuditDTO.getUserId())
                .consentType(consentAuditDTO.getConsentType())
                .consentStatus(consentAuditDTO.getConsentStatus())
                .ipAddress(consentAuditDTO.getIpAddress())
                .userAgent(consentAuditDTO.getUserAgent())
                .createdAt(consentAuditDTO.getCreatedAt())
                .build();
    }
}
