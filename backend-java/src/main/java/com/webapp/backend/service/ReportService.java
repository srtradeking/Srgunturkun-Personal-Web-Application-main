package com.webapp.backend.service;

import com.webapp.backend.dto.ReportsDTO;
import com.webapp.backend.model.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Report Service Interface
 */
public interface ReportService {

    /**
     * Create a report for a post
     */
    ReportsDTO reportPost(String userId, String postId, String category, String description, java.util.List<String> evidence);

    /**
     * Create a report for a comment
     */
    ReportsDTO reportComment(String userId, String commentId, String category, String description, java.util.List<String> evidence);

    /**
     * Create a report for a user
     */
    ReportsDTO reportUser(String userId, String reportedUserId, String category, String description, java.util.List<String> evidence);

    /**
     * Create a report for a message
     */
    ReportsDTO reportMessage(String userId, String messageId, String category, String description, java.util.List<String> evidence);

    /**
     * Get a specific report by ID and User ID (owner check)
     */
    Optional<ReportsDTO> getReport(String reportId, String userId);

    /**
     * Get all reports by a user
     */
    Page<ReportsDTO> getUserReports(String userId, Pageable pageable);

    /**
     * Get user's reports with a specific status
     */
    Page<ReportsDTO> getUserReportsByStatus(String userId, String status, Pageable pageable);

    /**
     * Appeal a dismissed report
     */
    ReportsDTO appealReport(String reportId, String appealDescription);

    /**
     * Check if a user can report (rate limiting)
     */
    boolean canUserReport(String userId);

    /**
     * Check if a duplicate report exists
     */
    boolean isDuplicateReport(String userId, String contentId);
}
