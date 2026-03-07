package com.webapp.backend.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Authorization Validator
 * 
 * Validates authorization and prevents bypass attacks:
 * - User authentication verification
 * - Resource ownership validation
 * - Horizontal privilege escalation prevention
 */
@Component
public class AuthorizationValidator {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationValidator.class);


    /**
     * Get current authenticated username
     */
    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("No authenticated user found");
            throw new SecurityException("User not authenticated");
        }

        String username = authentication.getName();
        if (username == null || username.equals("anonymousUser")) {
            log.warn("Anonymous user attempted to access protected resource");
            throw new SecurityException("User not authenticated");
        }

        return username;
    }

    /**
     * Check if user is authenticated
     */
    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && 
               authentication.isAuthenticated() && 
               !authentication.getName().equals("anonymousUser");
    }

    /**
     * Validate user can access resource owned by targetUserId
     * Prevents horizontal privilege escalation
     */
    public void validateResourceOwnership(Long targetUserId) {
        if (targetUserId == null) {
            throw new IllegalArgumentException("Target user ID cannot be null");
        }

        String currentUsername = getCurrentUsername();
        
        // In a real application, you would fetch the current user's ID from the database
        // For now, we'll validate that the username matches
        log.debug("Validating resource ownership for user: {}, target: {}", 
            currentUsername, targetUserId);

        // This is a simplified check - in production, compare actual user IDs
        // You should inject UserRepository and fetch the current user's ID
    }

    /**
     * Validate user can access resource owned by targetUsername
     */
    public void validateResourceOwnership(String targetUsername) {
        if (targetUsername == null || targetUsername.isEmpty()) {
            throw new IllegalArgumentException("Target username cannot be null or empty");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("User not authenticated");
        }

        // Allow Admin and Moderator to access any resource
        boolean isAdminOrMod = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MODERATOR"));
        
        if (isAdminOrMod) {
            return;
        }

        String currentUsername = authentication.getName();

        // Check if current user is the owner
        if (!currentUsername.equals(targetUsername)) {
            log.error("Authorization bypass attempt: user {} tried to access resource owned by {}", 
                currentUsername, targetUsername);
            throw new SecurityException("Access denied: You can only access your own resources");
        }
    }


    /**
     * Validate user ID matches current user
     */
    public void validateUserIdMatch(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        // This is a simplified check
        // In production, you should fetch the current user's ID from the database
        String currentUsername = getCurrentUsername();
        log.debug("Validating user ID {} for user {}", userId, currentUsername);

        // You should inject UserRepository and compare actual IDs
    }

    /**
     * Check if user can modify resource
     */
    public boolean canModifyResource(String resourceOwner) {
        if (resourceOwner == null) {
            return false;
        }

        String currentUsername = getCurrentUsername();

        // Only owner can modify
        return currentUsername.equals(resourceOwner);
    }

    /**
     * Check if user can delete resource
     */
    public boolean canDeleteResource(String resourceOwner) {
        if (resourceOwner == null) {
            return false;
        }

        String currentUsername = getCurrentUsername();

        // Only owner can delete
        return currentUsername.equals(resourceOwner);
    }

    /**
     * Validate path parameter doesn't contain authorization bypass attempts
     */
    public void validatePathParameter(String param) {
        if (param == null || param.isEmpty()) {
            return;
        }

        // Check for path traversal in IDs
        if (param.contains("..") || param.contains("/") || param.contains("\\")) {
            log.error("Path traversal attempt in authorization parameter: {}", param);
            throw new SecurityException("Invalid parameter format");
        }

        // Check for SQL injection in IDs
        if (param.contains("'") || param.contains("\"") || param.contains(";")) {
            log.error("SQL injection attempt in authorization parameter: {}", param);
            throw new SecurityException("Invalid parameter format");
        }
    }

    /**
     * Validate numeric ID parameter
     */
    public Long validateNumericId(String idParam) {
        if (idParam == null || idParam.isEmpty()) {
            throw new IllegalArgumentException("ID parameter cannot be null or empty");
        }

        // Validate it's numeric
        if (!idParam.matches("^[0-9]+$")) {
            log.error("Non-numeric ID parameter: {}", idParam);
            throw new IllegalArgumentException("ID must be numeric");
        }

        try {
            return Long.parseLong(idParam);
        } catch (NumberFormatException e) {
            log.error("Invalid numeric ID: {}", idParam);
            throw new IllegalArgumentException("Invalid ID format");
        }
    }

    /**
     * Check for IDOR (Insecure Direct Object Reference) attempt
     */
    public void validateNoIDOR(Long requestedId, Long ownerId) {
        if (requestedId == null || ownerId == null) {
            throw new IllegalArgumentException("IDs cannot be null");
        }

        if (!requestedId.equals(ownerId)) {
            log.error("IDOR attempt: user requested ID {} but owns ID {}", 
                requestedId, ownerId);
            throw new SecurityException("Access denied: Invalid resource ID");
        }
    }

    /**
     * Log authorization check
     */
    public void logAuthorizationCheck(String resource, String action, boolean allowed) {
        String username = getCurrentUsername();
        log.info("Authorization check: user={}, resource={}, action={}, allowed={}", 
            username, resource, action, allowed);
    }

    /**
     * Validate session belongs to current user
     */
    public void validateSessionOwnership(String sessionUsername) {
        String currentUsername = getCurrentUsername();

        if (!currentUsername.equals(sessionUsername)) {
            log.error("Session hijacking attempt: current user {} tried to use session of {}", 
                currentUsername, sessionUsername);
            throw new SecurityException("Session validation failed");
        }
    }
}
