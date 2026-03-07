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

/**
 * JavaScript Security Filter
 * 
 * Inspects HTTP requests for JavaScript-related security threats:
 * - Dangerous JavaScript patterns
 * - Prototype pollution attempts
 * - DOM-based XSS
 * - Template injection
 * - JSONP callback validation
 */
@Component
public class JavaScriptSecurityFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JavaScriptSecurityFilter.class);

    @Autowired
    private JavaScriptSecurityValidator jsSecurityValidator;

    // Parameters that might contain JavaScript
    private static final String[] JS_PARAMS = {
        "callback", "jsonp", "cb", "script", "code", "eval",
        "function", "handler", "onclick", "onerror", "onload"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        try {
            // Check all parameters for JavaScript threats
            Enumeration<String> paramNames = request.getParameterNames();
            while (paramNames.hasMoreElements()) {
                String paramName = paramNames.nextElement();
                String[] paramValues = request.getParameterValues(paramName);

                for (String paramValue : paramValues) {
                    if (paramValue != null && !paramValue.isEmpty()) {
                        // Check for dangerous JavaScript
                        if (jsSecurityValidator.containsDangerousJavaScript(paramValue)) {
                            log.error("Dangerous JavaScript in parameter {}: {}", 
                                paramName, paramValue);
                            sendSecurityError(response, "Dangerous JavaScript detected");
                            return;
                        }

                        // Check for prototype pollution
                        jsSecurityValidator.validatePrototypePollution(paramValue);

                        // Check for template injection
                        if (jsSecurityValidator.containsTemplateInjection(paramValue)) {
                            log.error("Template injection in parameter {}", paramName);
                            sendSecurityError(response, "Template injection detected");
                            return;
                        }

                        // Validate JSONP callbacks
                        if (isJSONPParameter(paramName)) {
                            jsSecurityValidator.validateJSONPCallback(paramValue);
                        }

                        // Validate URLs
                        if (paramName.toLowerCase().contains("url") || 
                            paramName.toLowerCase().contains("href") ||
                            paramName.toLowerCase().contains("redirect")) {
                            jsSecurityValidator.validateUrlProtocol(paramValue);
                        }
                    }
                }
            }

            // Check Content-Type header for JSON requests
            String contentType = request.getContentType();
            if (contentType != null && contentType.contains("application/json")) {
                // JSON requests will be validated by the controller
                log.debug("JSON request detected, will validate for prototype pollution");
            }

        } catch (SecurityException e) {
            log.error("JavaScript security violation: method={}, uri={}, message={}", 
                method, requestURI, e.getMessage());
            sendSecurityError(response, e.getMessage());
            return;
        } catch (Exception e) {
            log.error("Error during JavaScript security validation", e);
            sendSecurityError(response, "Request validation failed");
            return;
        }

        // Request is safe, continue
        filterChain.doFilter(request, response);
    }

    /**
     * Check if parameter is a JSONP callback
     */
    private boolean isJSONPParameter(String paramName) {
        if (paramName == null) {
            return false;
        }

        String lowerParam = paramName.toLowerCase();
        return lowerParam.equals("callback") || 
               lowerParam.equals("jsonp") ||
               lowerParam.equals("cb") ||
               lowerParam.contains("callback");
    }

    /**
     * Send security error response
     */
    private void sendSecurityError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/json");
        response.getWriter().write(String.format(
            "{\"error\": \"JavaScript security violation\", \"message\": \"%s\"}", 
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
               path.endsWith(".ico") ||
               path.endsWith(".png") ||
               path.endsWith(".jpg") ||
               path.endsWith(".gif") ||
               path.endsWith(".woff") ||
               path.endsWith(".ttf");
    }
}
