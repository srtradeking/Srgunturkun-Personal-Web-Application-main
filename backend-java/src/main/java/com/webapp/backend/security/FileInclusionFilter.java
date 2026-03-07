package com.webapp.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Enumeration;
import java.util.regex.Pattern;

/**
 * File Inclusion Filter
 * 
 * Inspects HTTP requests for file inclusion attempts in:
 * - Query parameters
 * - Request parameters
 * - Headers
 * - URI paths
 */
@Component
public class FileInclusionFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(FileInclusionFilter.class);

    @Autowired
    private FileInclusionValidator fileInclusionValidator;

    // Patterns that indicate file inclusion attempts
    private static final Pattern[] FILE_INCLUSION_PATTERNS = {
        Pattern.compile(".*\\.\\.[\\\\/].*"),              // Path traversal
        Pattern.compile(".*/etc/passwd.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*/etc/shadow.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\.\\./\\.\\./.*"),            // Multiple traversals
        Pattern.compile(".*%2e%2e[\\\\/].*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*file://.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*php://.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*data://.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*expect://.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*zip://.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\\\\\\\.*"),                   // UNC paths
        Pattern.compile(".*%00.*"),                        // Null byte
        Pattern.compile(".*\\.log$", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\.conf$", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\.config$", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*web\\.xml.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\.env.*", Pattern.CASE_INSENSITIVE)
    };

    // Parameter names commonly used in file inclusion attacks
    private static final String[] SUSPICIOUS_PARAM_NAMES = {
        "file", "path", "page", "template", "include", "require",
        "document", "folder", "root", "pg", "style", "pdf", "download",
        "lang", "language", "module", "view", "theme", "skin"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String queryString = request.getQueryString();

        // Check URI for file inclusion patterns
        if (containsFileInclusionPattern(requestURI)) {
            log.error("File inclusion attempt detected in URI: {}", requestURI);
            sendSecurityError(response, "Invalid request: potential file inclusion detected");
            return;
        }

        // Check query string
        if (queryString != null && containsFileInclusionPattern(queryString)) {
            log.error("File inclusion attempt detected in query string: {}", queryString);
            sendSecurityError(response, "Invalid request: potential file inclusion detected");
            return;
        }

        // Check suspicious parameters
        for (String paramName : SUSPICIOUS_PARAM_NAMES) {
            String paramValue = request.getParameter(paramName);
            if (paramValue != null && containsFileInclusionPattern(paramValue)) {
                log.error("File inclusion attempt detected in parameter {}: {}", paramName, paramValue);
                sendSecurityError(response, "Invalid request: potential file inclusion detected");
                return;
            }
        }

        // Check all parameters for file inclusion patterns
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            String[] paramValues = request.getParameterValues(paramName);
            
            for (String paramValue : paramValues) {
                if (paramValue != null && containsFileInclusionPattern(paramValue)) {
                    log.error("File inclusion attempt detected in parameter {}: {}", paramName, paramValue);
                    sendSecurityError(response, "Invalid request: potential file inclusion detected");
                    return;
                }
            }
        }

        // Check specific headers
        String[] headersToCheck = {"X-File-Path", "X-Include", "X-Template", "Referer"};
        for (String headerName : headersToCheck) {
            String headerValue = request.getHeader(headerName);
            if (headerValue != null && containsFileInclusionPattern(headerValue)) {
                log.error("File inclusion attempt detected in header {}: {}", headerName, headerValue);
                sendSecurityError(response, "Invalid request: potential file inclusion detected");
                return;
            }
        }

        // Request is safe, continue
        filterChain.doFilter(request, response);
    }

    /**
     * Check if input contains file inclusion patterns
     */
    private boolean containsFileInclusionPattern(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        for (Pattern pattern : FILE_INCLUSION_PATTERNS) {
            if (pattern.matcher(input).matches()) {
                return true;
            }
        }

        // Additional checks for common file inclusion indicators
        String lowerInput = input.toLowerCase();
        return lowerInput.contains("/etc/") ||
               lowerInput.contains("\\windows\\") ||
               lowerInput.contains("c:\\") ||
               lowerInput.contains("file://") ||
               lowerInput.contains("php://") ||
               lowerInput.contains("data://") ||
               lowerInput.contains("expect://") ||
               lowerInput.contains("zip://") ||
               lowerInput.contains("phar://") ||
               lowerInput.contains("../") ||
               lowerInput.contains("..\\") ||
               lowerInput.contains("%2e%2e") ||
               lowerInput.contains("%252e") ||
               lowerInput.contains("web-inf") ||
               lowerInput.contains("meta-inf") ||
               lowerInput.contains(".git/") ||
               lowerInput.contains(".svn/") ||
               lowerInput.contains(".env") ||
               lowerInput.contains("id_rsa") ||
               lowerInput.contains("authorized_keys");
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
               path.endsWith(".jpg") ||
               path.endsWith(".gif");
    }
}
