package com.webapp.backend.controller;

import com.webapp.backend.dto.ReportsDTO;
import com.webapp.backend.model.Report;
import com.webapp.backend.service.ReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Report Controller
 * REST endpoints for reporting system
 */
@Slf4j
@RestController
@RequestMapping("/reports")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * Submit a post report
     * POST /api/reports/posts
     */
    @PostMapping("/posts")
    public ResponseEntity<?> reportPost(@RequestBody ReportRequest request) {
        try {
            // For testing, use a hardcoded userId. In production, get from authentication
            String userId = "test-user";
            
            ReportsDTO report = reportService.reportPost(
                userId,
                request.getContentId(),
                request.getCategory(),
                request.getDescription(),
                request.getEvidence()
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(report);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(createErrorResponse("RATE_LIMIT_EXCEEDED", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(createErrorResponse("INVALID_REQUEST", e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating post report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("SERVER_ERROR", "Failed to create report"));
        }
    }

    /**
     * Submit a comment report
     * POST /api/reports/comments
     */
    @PostMapping("/comments")
    public ResponseEntity<?> reportComment(
            @RequestBody ReportRequest request,
            Authentication authentication) {
        try {
            String userId = authentication.getName();
            
            ReportsDTO report = reportService.reportComment(
                userId,
                request.getContentId(),
                request.getCategory(),
                request.getDescription(),
                request.getEvidence()
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(report);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(createErrorResponse("RATE_LIMIT_EXCEEDED", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(createErrorResponse("INVALID_REQUEST", e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating comment report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("SERVER_ERROR", "Failed to create report"));
        }
    }

    /**
     * Submit a user report
     * POST /api/reports/users
     */
    @PostMapping("/users")
    public ResponseEntity<?> reportUser(
            @RequestBody ReportRequest request,
            Authentication authentication) {
        try {
            String userId = authentication.getName();
            
            ReportsDTO report = reportService.reportUser(
                userId,
                request.getContentId(),
                request.getCategory(),
                request.getDescription(),
                request.getEvidence()
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(report);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(createErrorResponse("RATE_LIMIT_EXCEEDED", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(createErrorResponse("INVALID_REQUEST", e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating user report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("SERVER_ERROR", "Failed to create report"));
        }
    }

    /**
     * Submit a message report
     * POST /api/reports/messages
     */
    @PostMapping("/messages")
    public ResponseEntity<?> reportMessage(
            @RequestBody ReportRequest request,
            Authentication authentication) {
        try {
            String userId = authentication.getName();
            
            ReportsDTO report = reportService.reportMessage(
                userId,
                request.getContentId(),
                request.getCategory(),
                request.getDescription(),
                request.getEvidence()
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(report);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(createErrorResponse("RATE_LIMIT_EXCEEDED", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(createErrorResponse("INVALID_REQUEST", e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating message report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("SERVER_ERROR", "Failed to create report"));
        }
    }

    /**
     * Get a specific report by ID
     * GET /api/reports/{reportId}
     */
    @GetMapping("/{reportId}")
    public ResponseEntity<?> getReport(@PathVariable String reportId, Authentication authentication) {
        try {
            String userId = authentication.getName();
            Optional<ReportsDTO> report = reportService.getReport(reportId, userId);
            
            if (report.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("NOT_FOUND", "Report not found"));
            }
            
            return ResponseEntity.ok(report.get());
        } catch (Exception e) {
            log.error("Error retrieving report: {}", reportId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("SERVER_ERROR", "Failed to retrieve report"));
        }
    }

    /**
     * Get user's report history
     * GET /api/reports/user/{userId}?page=0&size=10
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserReports(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ReportsDTO> reports = reportService.getUserReports(userId, pageable);
            
            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            log.error("Error retrieving user reports: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("SERVER_ERROR", "Failed to retrieve reports"));
        }
    }

    /**
     * Appeal a dismissed report
     * POST /api/reports/{reportId}/appeal
     */
    @PostMapping("/{reportId}/appeal")
    public ResponseEntity<?> appealReport(
            @PathVariable String reportId,
            @RequestBody AppealRequest request,
            Authentication authentication) {
        try {
            ReportsDTO report = reportService.appealReport(reportId, request.getAppealDescription());
            
            return ResponseEntity.ok(report);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(createErrorResponse("NOT_FOUND", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(createErrorResponse("INVALID_STATE", e.getMessage()));
        } catch (Exception e) {
            log.error("Error appealing report: {}", reportId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("SERVER_ERROR", "Failed to appeal report"));
        }
    }

    /**
     * Check if user can report (rate limiting)
     * GET /api/reports/can-report/{userId}/{contentId}
     */
    @GetMapping("/can-report/{userId}/{contentId}")
    public ResponseEntity<?> canUserReport(
            @PathVariable String userId,
            @PathVariable String contentId) {
        try {
            boolean canReport = reportService.canUserReport(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("contentId", contentId);
            response.put("canReport", canReport);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error checking report eligibility: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("SERVER_ERROR", "Failed to check eligibility"));
        }
    }

    /**
     * Helper method to create error response
     */
    private Map<String, Object> createErrorResponse(String errorCode, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", errorCode);
        response.put("message", message);
        return response;
    }

    /**
     * Request DTO for report submission
     */
    public static class ReportRequest {
        private String contentId;
        private String category;
        private String description;
        private List<String> evidence;

        public String getContentId() {
            return contentId;
        }

        public void setContentId(String contentId) {
            this.contentId = contentId;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public List<String> getEvidence() {
            return evidence;
        }

        public void setEvidence(List<String> evidence) {
            this.evidence = evidence;
        }
    }

    /**
     * Request DTO for appeal submission
     */
    public static class AppealRequest {
        private String appealDescription;

        public String getAppealDescription() {
            return appealDescription;
        }

        public void setAppealDescription(String appealDescription) {
            this.appealDescription = appealDescription;
        }
    }
}
