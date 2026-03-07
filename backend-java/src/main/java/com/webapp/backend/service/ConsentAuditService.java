package com.webapp.backend.service;

import com.webapp.backend.mapper.ConsentAuditMapper;
import com.webapp.backend.dto.ConsentAuditDTO;
import com.webapp.backend.model.ConsentAudit;
import com.webapp.backend.repository.ConsentAuditRepository;
import com.webapp.backend.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing consent audits
 * Handles business logic for storing and retrieving consent records
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ConsentAuditService {

    private final ConsentAuditRepository consentAuditRepository;
    private final ConsentAuditMapper consentAuditMapper;
    private final UserProfileRepository userProfileRepository;

    /**
     * Create and store a new consent audit record
     * ONE-TIME INSERT ONLY - Direct write to private.consent_audit
     * No read-back or verification - application user only has INSERT permission
     * 
     * @param consentAuditDTO the consent data to store
     * @return the consent audit DTO with generated ID (if available) or null
     */
    public ConsentAuditDTO createConsentAudit(ConsentAuditDTO consentAuditDTO) {
        try {
            // Provide sensible defaults when not supplied by the frontend
            // Frontend currently sends a generic consent payload without explicit type/status fields.
            if (consentAuditDTO.getConsentType() == null) {
                // Single global consent record covering all categories
                consentAuditDTO.setConsentType("GLOBAL");
            }
            if (consentAuditDTO.getConsentStatus() == null) {
                // Current flows only record granted consent
                consentAuditDTO.setConsentStatus("GIVEN");
            }

            // Create a new consent audit record
            ConsentAudit consentAudit = consentAuditMapper.toEntity(consentAuditDTO);
            consentAudit.setCreatedAt(LocalDateTime.now());

            consentAuditRepository.save(consentAudit);

            log.info("Consent audit recorded for IP: {}, User: {}, Type: {}, Status: {}",
                    consentAudit.getIpAddress(),
                    consentAudit.getUserId() != null ? consentAudit.getUserId() : "ANONYMOUS",
                    consentAudit.getConsentType(),
                    consentAudit.getConsentStatus());

            return consentAuditMapper.toDTO(consentAudit);
        } catch (Exception e) {
            log.error("Error recording consent for user: {}", consentAuditDTO != null ? consentAuditDTO.getUserId() : "unknown", e);
            throw e;
        }
    }

    /**
     * Get consent history for a specific user
     */
    @Transactional(readOnly = true)
    public List<ConsentAuditDTO> getConsentHistory(String userId) {
        List<ConsentAudit> audits = consentAuditRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return audits.stream()
            .map(consentAuditMapper::toDTO)
            .collect(Collectors.toList());
    }

    /**
     * Get the latest consent from a user
     */
    @Transactional(readOnly = true)
    public Optional<ConsentAuditDTO> getLatestConsent(String userId) {
        return consentAuditRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
            .map(consentAuditMapper::toDTO);
    }

    /**
     * Get all consent audits by IP address (for security analysis)
     */
    @Transactional(readOnly = true)
    public List<ConsentAuditDTO> getConsentsByIpAddress(String ipAddress) {
        List<ConsentAudit> audits = consentAuditRepository.findByIpAddress(ipAddress);
        return audits.stream()
            .map(consentAuditMapper::toDTO)
            .collect(Collectors.toList());
    }

    /**
     * Delete all consent audits for a user (GDPR right to be forgotten)
     */
    public void deleteUserConsentHistory(String userId) {
        List<ConsentAudit> audits = consentAuditRepository.findByUserId(userId);
        consentAuditRepository.deleteAll(audits);
        
        log.info("Deleted all consent audits for user: {}", userId);
    }
}
