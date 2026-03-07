package com.webapp.backend.repository;

import com.webapp.backend.model.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Report Repository
 * Data access layer for Report entity
 * 
 * Database: personal_web_db
 * Schema: private
 * Table: reports
 * 
 * ID Type: UUID
 * Note: Report IDs, types, and statuses are now strings, not enums
 */
@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {

    /**
     * Find all reports by a specific user, ordered by most recent first
     */
    Page<Report> findByUserIdOrderByReportedAtDesc(String userId, Pageable pageable);

    Optional<Report> findByUserIdAndContentId(String userId, String contentId);

    /**
     * Count reports by a user within a time period
     */
    Long countByUserIdAndReportedAtAfter(String userId, LocalDateTime since);

    /**
     * Find all reports by user in a specific status
     */
    Page<Report> findByUserIdAndStatusOrderByReportedAtDesc(String userId, String status, Pageable pageable);

    /**
     * Find reports by category
     */
    Page<Report> findByCategoryOrderByReportedAtDesc(String category, Pageable pageable);

    /**
     * Check if a report exists for a specific content
     */
    boolean existsByContentIdAndUserId(String contentId, String userId);
}

