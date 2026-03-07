package com.webapp.backend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * CSP Violation Report Controller
 * 
 * Receives and logs Content Security Policy violation reports
 * from browsers. This helps identify:
 * - Legitimate CSP violations (bugs)
 * - Attack attempts
 * - CSP bypass attempts
 */
@RestController
@RequestMapping("/csp-report")
@CrossOrigin(origins = "*")
public class CSPViolationReportController {

    private static final Logger log = LoggerFactory.getLogger(CSPViolationReportController.class);

    /**
     * Receive CSP violation reports
     * POST /api/csp-report
     */
    @PostMapping
    public ResponseEntity<Void> reportViolation(@RequestBody Map<String, Object> report) {
        try {
            // Extract violation details
            @SuppressWarnings("unchecked")
            Map<String, Object> cspReport = (Map<String, Object>) report.get("csp-report");
            
            if (cspReport != null) {
                String documentUri = (String) cspReport.get("document-uri");
                String violatedDirective = (String) cspReport.get("violated-directive");
                String blockedUri = (String) cspReport.get("blocked-uri");
                String sourceFile = (String) cspReport.get("source-file");
                Integer lineNumber = (Integer) cspReport.get("line-number");
                Integer columnNumber = (Integer) cspReport.get("column-number");

                // Log the violation
                log.warn("CSP Violation Report: " +
                        "document={}, directive={}, blocked={}, source={}:{}:{}", 
                        documentUri, violatedDirective, blockedUri, 
                        sourceFile, lineNumber, columnNumber);

                // Check for potential attack patterns
                if (isLikelyAttack(cspReport)) {
                    log.error("Potential CSP bypass attack detected: {}", cspReport);
                }
            } else {
                log.warn("Received CSP report with no csp-report field: {}", report);
            }

            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            log.error("Error processing CSP violation report", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Check if CSP violation indicates a likely attack
     */
    private boolean isLikelyAttack(Map<String, Object> cspReport) {
        String blockedUri = (String) cspReport.get("blocked-uri");
        String violatedDirective = (String) cspReport.get("violated-directive");

        if (blockedUri == null || violatedDirective == null) {
            return false;
        }

        String lowerBlocked = blockedUri.toLowerCase();
        String lowerDirective = violatedDirective.toLowerCase();

        // Check for common attack patterns
        return lowerBlocked.contains("javascript:") ||
               lowerBlocked.contains("data:text/html") ||
               lowerBlocked.contains("vbscript:") ||
               (lowerDirective.contains("script-src") && lowerBlocked.contains("eval")) ||
               lowerBlocked.contains("<script") ||
               lowerBlocked.contains("onerror=") ||
               lowerBlocked.contains("onclick=");
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "CSP Violation Reporter"
        ));
    }
}
