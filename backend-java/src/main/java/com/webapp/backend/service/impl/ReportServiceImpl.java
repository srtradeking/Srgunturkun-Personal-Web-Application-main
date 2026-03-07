package com.webapp.backend.service.impl;

import com.webapp.backend.dto.ReportsDTO;
import com.webapp.backend.model.Report;
import com.webapp.backend.repository.ReportRepository;
import com.webapp.backend.service.ReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Report Service Implementation
 * Handles all business logic for reporting system
 */
@Slf4j
@Service
@Transactional
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;

    // Rate limiting: reports per hour
    private static final int REPORTS_PER_HOUR_LIMIT = 5;

    public ReportServiceImpl(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    @Override
    public ReportsDTO reportPost(String userId, String postId, String category, String description, List<String> evidence) {
        return createReport(userId, postId, "POST", category, description, evidence);
    }

    @Override
    public ReportsDTO reportComment(String userId, String commentId, String category, String description, List<String> evidence) {
        return createReport(userId, commentId, "COMMENT", category, description, evidence);
    }

    @Override
    public ReportsDTO reportUser(String userId, String reportedUserId, String category, String description, List<String> evidence) {
        // Prevent self-reporting
        if (userId.equals(reportedUserId)) {
            throw new IllegalArgumentException("Cannot report yourself");
        }
        return createReport(userId, reportedUserId, "USER", category, description, evidence);
    }

    @Override
    public ReportsDTO reportMessage(String userId, String messageId, String category, String description, List<String> evidence) {
        return createReport(userId, messageId, "MESSAGE", category, description, evidence);
    }

    /**
     * Internal method to create a report with validation
     */
    private ReportsDTO createReport(String userId, String contentId, String type, String category, String description, List<String> evidence) {
        // Rate limiting check
        if (!canUserReport(userId)) {
            throw new IllegalStateException("You have reached the report limit. Please try again later.");
        }

        // Duplicate check
        if (isDuplicateReport(userId, contentId)) {
            throw new IllegalStateException("You have already reported this content");
        }

        // Create report
        Report report = Report.builder()
            .type(type)
            .category(category)
            .contentId(contentId)
            .userId(userId)
            .description(description)
            .status("PENDING")
            .reportedAt(LocalDateTime.now())
            .isActive(true)
            .hasAppealed(false)
            .build();

        Report savedReport = reportRepository.save(report);
        log.info("Report created: ID={}, Type={}, Category={}, ContentId={}, ReportedBy={}", 
            savedReport.getId(), type, category, contentId, userId);

        return ReportsDTO.fromEntity(savedReport);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReportsDTO> getReport(String reportId, String userId) {
        java.util.UUID reportIdUuid = java.util.UUID.fromString(reportId);
        return reportRepository.findById(reportIdUuid)
            .filter(report -> report.getUserId().equals(userId))
            .map(ReportsDTO::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReportsDTO> getUserReports(String userId, Pageable pageable) {
        return reportRepository.findByUserIdOrderByReportedAtDesc(userId, pageable)
            .map(ReportsDTO::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReportsDTO> getUserReportsByStatus(String userId, String status, Pageable pageable) {
        return reportRepository.findByUserIdAndStatusOrderByReportedAtDesc(userId, status, pageable)
            .map(ReportsDTO::fromEntity);
    }

    @Override
    public ReportsDTO appealReport(String reportId, String appealDescription) {
        java.util.UUID reportIdUuid = java.util.UUID.fromString(reportId);
        Report report = reportRepository.findById(reportIdUuid)
            .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        // Only dismissed reports can be appealed
        if (!"DISMISSED".equals(report.getStatus())) {
            throw new IllegalStateException("Only dismissed reports can be appealed");
        }

        // Prevent multiple appeals
        if (report.getHasAppealed()) {
            throw new IllegalStateException("This report has already been appealed");
        }

        report.setHasAppealed(true);
        report.setAppealDescription(appealDescription);
        report.setAppealedAt(LocalDateTime.now());
        report.setStatus("APPEALED");

        Report updatedReport = reportRepository.save(report);
        log.info("Report appealed: ID={}, AppealedAt={}", reportId, LocalDateTime.now());

        return ReportsDTO.fromEntity(updatedReport);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canUserReport(String userId) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        Long reportCount = reportRepository.countByUserIdAndReportedAtAfter(userId, oneHourAgo);
        return reportCount < REPORTS_PER_HOUR_LIMIT;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isDuplicateReport(String userId, String contentId) {
        return reportRepository.existsByContentIdAndUserId(contentId, userId);
    }
}
