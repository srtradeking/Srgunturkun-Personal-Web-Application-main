package com.webapp.backend.controller;

import com.webapp.backend.security.CsrfTokenService;
import com.webapp.backend.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * CSRF Token Controller
 * 
 * Provides endpoints for CSRF token management.
 * Clients should request a CSRF token after authentication
 * and include it in all state-changing requests.
 */
@RestController
@RequestMapping("/csrf")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "CSRF", description = "CSRF token management")
public class CsrfController {

    private static final Logger log = LoggerFactory.getLogger(CsrfController.class);

    @Autowired
    private CsrfTokenService csrfTokenService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Get CSRF token for authenticated user
     * GET /api/csrf/token
     * 
     * Requires: JWT token in Authorization header
     * Returns: CSRF token and expiry time
     */
    @GetMapping("/token")
    @Operation(summary = "Get CSRF token", description = "Generate a new CSRF token for the authenticated user")
    public ResponseEntity<?> getCsrfToken(@RequestHeader("Authorization") String authHeader) {
        try {
            // Validate Authorization header
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401)
                    .body(Map.of("error", "Authorization header required"));
            }

            String jwtToken = authHeader.substring(7);
            
            // Validate JWT token
            if (!jwtUtil.validateToken(jwtToken)) {
                return ResponseEntity.status(401)
                    .body(Map.of("error", "Invalid or expired JWT token"));
            }

            // Extract username from JWT
            String username = jwtUtil.extractUsername(jwtToken);
            
            // Generate CSRF token
            String csrfToken = csrfTokenService.generateToken(username);
            long expiryMillis = csrfTokenService.getTokenExpiryMillis();

            Map<String, Object> response = new HashMap<>();
            response.put("token", csrfToken);
            response.put("expiresIn", expiryMillis);
            response.put("headerName", "X-CSRF-Token");
            response.put("paramName", "_csrf");

            log.info("CSRF token generated for user: {}", username);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error generating CSRF token", e);
            return ResponseEntity.status(500)
                .body(Map.of("error", "Failed to generate CSRF token"));
        }
    }

    /**
     * Invalidate CSRF token
     * DELETE /api/csrf/token
     * 
     * Requires: JWT token in Authorization header
     * Optional: CSRF token to invalidate specific token
     */
    @DeleteMapping("/token")
    @Operation(summary = "Invalidate CSRF token", description = "Invalidate the current CSRF token")
    public ResponseEntity<?> invalidateCsrfToken(
            @RequestHeader("Authorization") String authHeader,
            @RequestHeader(value = "X-CSRF-Token", required = false) String csrfToken) {
        try {
            // Validate Authorization header
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401)
                    .body(Map.of("error", "Authorization header required"));
            }

            String jwtToken = authHeader.substring(7);
            
            // Validate JWT token
            if (!jwtUtil.validateToken(jwtToken)) {
                return ResponseEntity.status(401)
                    .body(Map.of("error", "Invalid or expired JWT token"));
            }

            // Extract username from JWT
            String username = jwtUtil.extractUsername(jwtToken);

            if (csrfToken != null && !csrfToken.isEmpty()) {
                // Invalidate specific token
                csrfTokenService.invalidateToken(csrfToken);
                log.info("CSRF token invalidated for user: {}", username);
            } else {
                // Invalidate all tokens for user
                csrfTokenService.invalidateUserTokens(username);
                log.info("All CSRF tokens invalidated for user: {}", username);
            }

            return ResponseEntity.ok(Map.of("message", "CSRF token invalidated successfully"));

        } catch (Exception e) {
            log.error("Error invalidating CSRF token", e);
            return ResponseEntity.status(500)
                .body(Map.of("error", "Failed to invalidate CSRF token"));
        }
    }

    /**
     * Validate CSRF token (for testing purposes)
     * POST /api/csrf/validate
     */
    @PostMapping("/validate")
    @Operation(summary = "Validate CSRF token", description = "Check if a CSRF token is valid")
    public ResponseEntity<?> validateCsrfToken(
            @RequestHeader("Authorization") String authHeader,
            @RequestHeader("X-CSRF-Token") String csrfToken) {
        try {
            // Validate Authorization header
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401)
                    .body(Map.of("error", "Authorization header required"));
            }

            String jwtToken = authHeader.substring(7);
            
            // Validate JWT token
            if (!jwtUtil.validateToken(jwtToken)) {
                return ResponseEntity.status(401)
                    .body(Map.of("error", "Invalid or expired JWT token"));
            }

            // Extract username from JWT
            String username = jwtUtil.extractUsername(jwtToken);

            // Validate CSRF token
            boolean isValid = csrfTokenService.validateToken(csrfToken, username);

            Map<String, Object> response = new HashMap<>();
            response.put("valid", isValid);
            response.put("message", isValid ? "CSRF token is valid" : "CSRF token is invalid or expired");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error validating CSRF token", e);
            return ResponseEntity.status(500)
                .body(Map.of("error", "Failed to validate CSRF token"));
        }
    }
}
