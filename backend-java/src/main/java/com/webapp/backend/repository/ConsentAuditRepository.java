package com.webapp.backend.repository;

import com.webapp.backend.model.ConsentAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for ConsentAudit entity
 * Handles database operations for consent audit records
 */
@Repository
public interface ConsentAuditRepository extends JpaRepository<ConsentAudit, Long> {

    /**
     * Find all consent audits for a specific user
     */
    List<ConsentAudit> findByUserId(String userId);

    /**
     * Find all consent audits for a user ordered by creation date (newest first)
     */
    List<ConsentAudit> findByUserIdOrderByCreatedAtDesc(String userId);

    /**
     * Find the latest (most recent) consent from a user
     */
    Optional<ConsentAudit> findFirstByUserIdOrderByCreatedAtDesc(String userId);

    /**
     * Find the currently active consent for a user
     */
    Optional<ConsentAudit> findByUserIdAndConsentStatusOrderByCreatedAtDesc(String userId, String consentStatus);

    /**
     * Find all consent audits by IP address (for security analysis)
     */
    List<ConsentAudit> findByIpAddress(String ipAddress);

    /**
     * Find the most recent consent for an IP address
     * Used for returning visitor consent detection
     * @param ipAddress The IP address to check
     * @return The most recent consent for this IP
     */
    Optional<ConsentAudit> findFirstByIpAddressOrderByCreatedAtDesc(String ipAddress);

    /**
     * Find consent audits within a date range
     */
    @Query("SELECT ca FROM ConsentAudit ca WHERE ca.createdAt BETWEEN :startDate AND :endDate ORDER BY ca.createdAt DESC")
    List<ConsentAudit> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Find consent audits for a user within a date range
     */
    @Query("SELECT ca FROM ConsentAudit ca WHERE ca.userId = :userId AND ca.createdAt BETWEEN :startDate AND :endDate ORDER BY ca.createdAt DESC")
    List<ConsentAudit> findByUserIdAndDateRange(@Param("userId") String userId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Find all rejected consents (for compliance review)
     */
    @Query("SELECT ca FROM ConsentAudit ca WHERE ca.consentStatus = 'WITHDRAWN' ORDER BY ca.createdAt DESC")
    List<ConsentAudit> findAllRejectedConsents();

    /**
     * Count how many users gave consent
     */
    @Query("SELECT COUNT(DISTINCT ca.userId) FROM ConsentAudit ca WHERE ca.consentStatus = 'GIVEN'")
    long countUsersWhoConsented();

    /**
     * Count how many users rejected consent
     */
    @Query("SELECT COUNT(DISTINCT ca.userId) FROM ConsentAudit ca WHERE ca.consentStatus = 'WITHDRAWN'")
    long countUsersWhoRejected();

    /**
     * Get consent audit history for a user in the last N days
     */
    @Query("SELECT ca FROM ConsentAudit ca WHERE ca.userId = :userId AND ca.createdAt >= :fromDate ORDER BY ca.createdAt DESC")
    List<ConsentAudit> findRecentConsentHistory(@Param("userId") String userId, @Param("fromDate") LocalDateTime fromDate);
}
