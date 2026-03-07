package com.webapp.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Enumeration;
import java.util.regex.Pattern;

/**
 * Command Injection Filter
 * 
 * Inspects HTTP requests for potential command injection attempts
 * in query parameters, headers, and paths.
 */
@Component
public class CommandInjectionFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CommandInjectionFilter.class);

    // Patterns for command injection detection
    private static final Pattern[] DANGEROUS_PATTERNS = {
        Pattern.compile(".*[;&|`$].*"),                    // Shell metacharacters
        Pattern.compile(".*\\$\\(.*\\).*"),                // Command substitution
        Pattern.compile(".*`.*`.*"),                       // Backtick command execution
        Pattern.compile(".*(exec|eval|system|passthru|shell_exec)\\s*\\(.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*(cmd|bash|sh|powershell)\\.exe.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\|\\|.*"),                     // OR operator
        Pattern.compile(".*&&.*"),                         // AND operator
        Pattern.compile(".*>.*"),                          // Redirect
        Pattern.compile(".*<.*")                           // Redirect
    };

    // Patterns for path traversal
    private static final Pattern[] PATH_TRAVERSAL_PATTERNS = {
        Pattern.compile(".*\\.\\.[\\\\/].*"),
        Pattern.compile(".*%2e%2e[\\\\/].*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*%252e%252e[\\\\/].*", Pattern.CASE_INSENSITIVE)
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String queryString = request.getQueryString();

        // Check URI for dangerous patterns
        if (containsDangerousPattern(requestURI)) {
            log.error("Command injection attempt detected in URI: {}", requestURI);
            sendSecurityError(response, "Invalid request: potential security threat detected");
            return;
        }

        // Check all parameters
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            String[] paramValues = request.getParameterValues(paramName);
            
            for (String paramValue : paramValues) {
                if (containsDangerousPattern(paramValue)) {
                    log.error("Command injection attempt detected in parameter {}: {}", paramName, paramValue);
                    sendSecurityError(response, "Invalid request: potential security threat detected");
                    return;
                }
            }
        }

        // Check specific headers that might be used in file operations
        String[] headersToCheck = {"X-File-Name", "X-File-Path", "Content-Disposition"};
        for (String headerName : headersToCheck) {
            String headerValue = request.getHeader(headerName);
            if (headerValue != null && containsDangerousPattern(headerValue)) {
                log.error("Command injection attempt detected in header {}: {}", headerName, headerValue);
                sendSecurityError(response, "Invalid request: potential security threat detected");
                return;
            }
        }

        // Request is safe, continue
        filterChain.doFilter(request, response);
    }

    /**
     * Check if input contains dangerous patterns
     */
    private boolean containsDangerousPattern(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        // Check command injection patterns
        for (Pattern pattern : DANGEROUS_PATTERNS) {
            if (pattern.matcher(input).matches()) {
                return true;
            }
        }

        // Check path traversal patterns
        for (Pattern pattern : PATH_TRAVERSAL_PATTERNS) {
            if (pattern.matcher(input).matches()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Send security error response
     */
    private void sendSecurityError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/json");
        response.getWriter().write(String.format("{\"error\": \"%s\"}", message));
    }

    /**
     * Skip filter for certain paths (e.g., static resources)
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/static/") || 
               path.startsWith("/public/") ||
               path.startsWith("/webjars/") ||
               path.endsWith(".css") ||
               path.endsWith(".js") ||
               path.endsWith(".ico");
    }
}
