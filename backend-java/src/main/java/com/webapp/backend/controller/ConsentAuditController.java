package com.webapp.backend.controller;

import com.webapp.backend.dto.ConsentAuditDTO;
import com.webapp.backend.service.ConsentAuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for consent audit operations
 * Handles API endpoints for storing consent records
 * 
 * NOTE: Application user has INSERT-only access to private.consent_audit table
 * Only the POST endpoint (createConsentAudit) should be used in production.
 * Other endpoints (GET, DELETE) are disabled for security compliance.
 */
@RestController
@RequestMapping("/consent-audits")
@RequiredArgsConstructor
@Slf4j
public class ConsentAuditController {

    private final ConsentAuditService consentAuditService;

    /**
     * POST /api/consent-audits
     * Create a new consent audit record with IP address
     * This is the ONLY permitted operation for the application user (INSERT privilege)
     */
    @PostMapping
    public ResponseEntity<ConsentAuditDTO> createConsentAudit(
            @RequestBody ConsentAuditDTO consentAuditDTO,
            HttpServletRequest request) {
        try {
            // Extract IP address from request
            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            
            // Set IP and User-Agent if not already provided
            if (consentAuditDTO.getIpAddress() == null) {
                consentAuditDTO.setIpAddress(ipAddress);
            }
            if (consentAuditDTO.getUserAgent() == null) {
                consentAuditDTO.setUserAgent(userAgent);
            }

            ConsentAuditDTO saved = consentAuditService.createConsentAudit(consentAuditDTO);

            log.info("Consent audit created: User={}, Status={}, IP={}",
                    saved.getUserId(),
                    saved.getConsentStatus(),
                    saved.getIpAddress());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            log.error("Error creating consent audit", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/consent-audits/user/{userId}
     * DISABLED - Application user has INSERT-only access to private.consent_audit
     * Queries must be executed by postgres superuser directly
     */
    // @GetMapping("/user/{userId}")
    // public ResponseEntity<List<ConsentAuditDTO>> getUserConsentHistory(@PathVariable String userId)

    /**
     * GET /api/consent-audits/user/{userId}/latest
     * DISABLED - Application user has INSERT-only access to private.consent_audit
     */
    // @GetMapping("/user/{userId}/latest")
    // public ResponseEntity<ConsentAuditDTO> getLatestConsent(@PathVariable String userId)

    /**
     * GET /api/consent-audits/user/{userId}/active
     * DISABLED - Application user has INSERT-only access to private.consent_audit
     */
    // @GetMapping("/user/{userId}/active")
    // public ResponseEntity<ConsentAuditDTO> getActiveConsent(@PathVariable String userId)

    /**
     * GET /api/consent-audits/by-ip/{ipAddress}
     * Get the most recent active consent for a returning visitor (by IP address)
     * Used to skip consent wall for returning visitors
     * 
     * @param ipAddress The IP address to check
     * @return ConsentAuditDTO if found, 404 if not found
     */
    @GetMapping("/by-ip/{ipAddress}")
    public ResponseEntity<List<ConsentAuditDTO>> getConsentByIP(@PathVariable String ipAddress) {
        try {
            log.debug("Checking consents for IP: {}", ipAddress);
            var consents = consentAuditService.getConsentsByIpAddress(ipAddress);
            
            if (consents != null && !consents.isEmpty()) {
                log.info("Existing consents found for IP: {}", ipAddress);
                return ResponseEntity.ok(consents);
            }
            
            log.debug("No consent found for IP: {}", ipAddress);
            return ResponseEntity.ok(List.of());
        } catch (Exception e) {
            log.error("Error checking consent for IP: {}", ipAddress, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/consent-audits/ip/{ipAddress}
     * DISABLED - Application user has INSERT-only access to private.consent_audit
     * To query by IP, use postgres and execute:
     * SELECT * FROM private.consent_audit WHERE ip_address = inet '{ipAddress}'
     */
    // @GetMapping("/ip/{ipAddress}")
    // public ResponseEntity<List<ConsentAuditDTO>> getConsentsByIpAddress(@PathVariable String ipAddress)

    /**
     * GET /api/consent-audits/statistics
     * DISABLED - Application user has INSERT-only access to private.consent_audit
     * To get statistics, use postgres and execute:
     * SELECT consent_given, COUNT(*) as total FROM private.consent_audit GROUP BY consent_given
     */
    // @GetMapping("/statistics")
    // public ResponseEntity<ConsentAuditService.ConsentStatistics> getStatistics()

    /**
     * GET /api/consent-audits/rejected
     * DISABLED - Application user has INSERT-only access to private.consent_audit
     */
    // @GetMapping("/rejected")
    // public ResponseEntity<List<ConsentAuditDTO>> getRejectedConsents()

    /**
     * DELETE /api/consent-audits/user/{userId}
     * DISABLED - Application user has INSERT-only access to private.consent_audit
     * GDPR deletions must be executed by postgres superuser:
     */
    // @DeleteMapping("/user/{userId}")
    // public ResponseEntity<Map<String, String>> deleteUserConsentHistory(@PathVariable String userId)

    /**
     * Extract client IP address from HTTP request
     * Handles proxies and load balancers
     */
    private String getClientIpAddress(HttpServletRequest request) {
        // Try X-Forwarded-For header first (for proxies/load balancers)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        // Try X-Real-IP header (Nginx proxy)
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        // Fall back to remote address
        return request.getRemoteAddr();
    }
}
