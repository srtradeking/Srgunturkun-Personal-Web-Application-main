package com.webapp.backend.security;

import com.webapp.backend.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * CSRF Protection Filter
 * 
 * Validates CSRF tokens for state-changing operations (POST, PUT, DELETE, PATCH).
 * Uses double-submit cookie pattern with server-side validation.
 * 
 * For JWT-based stateless authentication, CSRF protection is applied to:
 * - Authenticated state-changing requests
 * - Requests that modify server state
 */
@Component
public class CsrfProtectionFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CsrfProtectionFilter.class);

    private static final String CSRF_TOKEN_HEADER = "X-CSRF-Token";
    private static final String CSRF_TOKEN_PARAM = "_csrf";
    
    // HTTP methods that require CSRF protection
    private static final List<String> PROTECTED_METHODS = Arrays.asList(
        HttpMethod.POST.name(),
        HttpMethod.PUT.name(),
        HttpMethod.DELETE.name(),
        HttpMethod.PATCH.name()
    );

    @Autowired
    private CsrfTokenService csrfTokenService;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String method = request.getMethod();
        String requestURI = request.getRequestURI();

        // Skip CSRF check for safe methods (GET, HEAD, OPTIONS)
        if (!PROTECTED_METHODS.contains(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Skip CSRF check for public endpoints (login, register, etc.)
        if (isPublicEndpoint(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract JWT token to get user ID
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // No JWT token, let other filters handle authentication
            filterChain.doFilter(request, response);
            return;
        }

        String jwtToken = authHeader.substring(7);
        
        // Validate JWT first
        if (!jwtUtil.validateToken(jwtToken)) {
            log.warn("CSRF check skipped: Invalid JWT token");
            filterChain.doFilter(request, response);
            return;
        }

        // Extract user ID from JWT
        String username = jwtUtil.extractUsername(jwtToken);
        
        // Get CSRF token from header or parameter
        String csrfToken = request.getHeader(CSRF_TOKEN_HEADER);
        if (csrfToken == null) {
            csrfToken = request.getParameter(CSRF_TOKEN_PARAM);
        }

        // Validate CSRF token
        if (csrfToken == null || csrfToken.isEmpty()) {
            log.warn("CSRF validation failed: No CSRF token provided for {} {}", method, requestURI);
            sendCsrfError(response, "CSRF token is required");
            return;
        }

        if (!csrfTokenService.validateToken(csrfToken, username)) {
            log.error("CSRF validation failed: Invalid or expired token for user: {} on {} {}", 
                username, method, requestURI);
            sendCsrfError(response, "Invalid or expired CSRF token");
            return;
        }

        log.debug("CSRF token validated successfully for user: {} on {} {}", username, method, requestURI);
        
        // CSRF token is valid, continue with request
        filterChain.doFilter(request, response);
    }

    /**
     * Check if endpoint is public (doesn't require CSRF protection)
     */
    private boolean isPublicEndpoint(String uri) {
        return uri.contains("/auth/login") ||
               uri.contains("/auth/register") ||
               uri.contains("/auth/refresh") ||
               uri.contains("/auth/verify-email") ||
               uri.contains("/auth/resend-verification") ||
               uri.contains("/health") ||
               uri.contains("/actuator") ||
               uri.contains("/swagger") ||
               uri.contains("/api-docs") ||
               uri.contains("/h2-console") ||
               uri.contains("/public/") ||
               uri.contains("/reports");
    }

    /**
     * Send CSRF error response
     */
    private void sendCsrfError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write(String.format(
            "{\"error\": \"CSRF validation failed\", \"message\": \"%s\"}", 
            message
        ));
    }

    /**
     * Skip filter for certain paths
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Skip for static resources
        return path.startsWith("/static/") || 
               path.startsWith("/webjars/") ||
               path.endsWith(".css") ||
               path.endsWith(".js") ||
               path.endsWith(".ico") ||
               path.endsWith(".png") ||
               path.endsWith(".jpg");
    }
}
