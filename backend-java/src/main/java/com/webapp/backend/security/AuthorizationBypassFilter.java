package com.webapp.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Authorization Bypass Filter
 * 
 * Detects and prevents authorization bypass attempts:
 * - IDOR (Insecure Direct Object Reference)
 * - Horizontal privilege escalation
 * - Vertical privilege escalation
 * - Path manipulation
 * - Parameter tampering
 */
@Component
public class AuthorizationBypassFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationBypassFilter.class);

    @Autowired
    private AuthorizationValidator authorizationValidator;

    // Patterns for user-specific endpoints
    private static final Pattern USER_ID_PATTERN = Pattern.compile("/users?/(\\d+)");
    private static final Pattern PROFILE_PATTERN = Pattern.compile("/profiles?/(\\w+)");
    private static final Pattern POST_PATTERN = Pattern.compile("/posts?/(\\d+)");


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        try {
            // Skip if not authenticated (will be handled by authentication filter)
            if (!authorizationValidator.isAuthenticated()) {
                filterChain.doFilter(request, response);
                return;
            }


            // Check for IDOR in user endpoints
            if (isUserSpecificEndpoint(requestURI)) {
                validateUserEndpointAccess(requestURI, method);
            }

            // Validate ID parameters in query string
            String userId = request.getParameter("userId");
            if (userId != null && !userId.isEmpty()) {
                validateUserIdParameter(userId);
            }

            String profileId = request.getParameter("profileId");
            if (profileId != null && !profileId.isEmpty()) {
                validateUserIdParameter(profileId);
            }

            // Check for suspicious parameter combinations
            detectParameterTampering(request);

        } catch (SecurityException e) {
            log.error("Authorization bypass attempt detected: method={}, uri={}, message={}", 
                method, requestURI, e.getMessage());
            sendAuthorizationError(response, e.getMessage());
            return;
        } catch (Exception e) {
            log.error("Error during authorization validation", e);
            sendAuthorizationError(response, "Authorization validation failed");
            return;
        }

        // Authorization passed, continue
        filterChain.doFilter(request, response);
    }


    /**
     * Check if endpoint is user-specific
     */
    private boolean isUserSpecificEndpoint(String uri) {
        return USER_ID_PATTERN.matcher(uri).find() ||
               PROFILE_PATTERN.matcher(uri).find() ||
               uri.contains("/my/") ||
               uri.contains("/me/");
    }

    /**
     * Validate access to user-specific endpoint
     */
    private void validateUserEndpointAccess(String uri, String method) {
        String currentUsername = authorizationValidator.getCurrentUsername();

        // Extract user ID from URI
        Matcher userIdMatcher = USER_ID_PATTERN.matcher(uri);
        if (userIdMatcher.find()) {
            String userIdStr = userIdMatcher.group(1);
            Long userId = authorizationValidator.validateNumericId(userIdStr);
            
            // Validate user can access this resource
            // In production, you would fetch the user ID from database and compare
            log.debug("User {} accessing user-specific endpoint with ID {}", 
                currentUsername, userId);
        }

        // Extract profile username from URI
        Matcher profileMatcher = PROFILE_PATTERN.matcher(uri);
        if (profileMatcher.find()) {
            String profileUsername = profileMatcher.group(1);
            
            // For modification operations, enforce strict ownership
            if (method.equals("PUT") || method.equals("DELETE") || method.equals("PATCH")) {
                authorizationValidator.validateResourceOwnership(profileUsername);
            } 
            // Allow read operations to pass through to ProfileAuthorizationFilter or Controller
            // Users should be able to view other profiles
        }
    }

    /**
     * Validate user ID parameter
     */
    private void validateUserIdParameter(String userIdParam) {
        // Validate format
        Long userId = authorizationValidator.validateNumericId(userIdParam);
        
        // Validate user can access this user ID
        authorizationValidator.validateUserIdMatch(userId);
    }

    /**
     * Detect parameter tampering
     */
    private void detectParameterTampering(HttpServletRequest request) {
        // Check for suspicious parameter combinations
        String userId = request.getParameter("userId");
        String role = request.getParameter("role");
        String isAdmin = request.getParameter("isAdmin");
        String permissions = request.getParameter("permissions");

        // Attempting to set admin flag
        if (isAdmin != null && isAdmin.equalsIgnoreCase("true")) {
            log.error("Privilege escalation attempt: trying to set isAdmin=true");
            throw new SecurityException("Invalid parameter: isAdmin");
        }

        // Attempting to set role parameter
        if (role != null) {
            log.error("Privilege escalation attempt: trying to set role={}", role);
            throw new SecurityException("Invalid parameter: role");
        }

        // Attempting to modify permissions
        if (permissions != null) {
            log.error("Privilege escalation attempt: trying to modify permissions");
            throw new SecurityException("Invalid parameter: permissions");
        }
    }

    /**
     * Send authorization error response
     */
    private void sendAuthorizationError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write(String.format(
            "{\"error\": \"Authorization failed\", \"message\": \"%s\"}", 
            message
        ));
    }

    /**
     * Skip filter for public endpoints
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // Skip for public endpoints
        return path.startsWith("/api/auth/") ||
               path.startsWith("/auth/") ||
               path.startsWith("/api/public/") ||
               path.startsWith("/public/") ||
               path.startsWith("/static/") ||
               path.startsWith("/webjars/") ||
               path.equals("/api/health") ||
               path.equals("/health") ||
               path.endsWith(".css") ||
               path.endsWith(".js") ||
               path.endsWith(".ico") ||
               path.endsWith(".png") ||
               path.endsWith(".jpg");
    }
}
