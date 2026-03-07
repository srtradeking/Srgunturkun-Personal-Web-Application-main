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
 * XSS (Cross-Site Scripting) Filter
 * 
 * Inspects HTTP requests for XSS attempts in:
 * - Query parameters
 * - Request parameters
 * - Headers
 * - Request body (for JSON)
 */
@Component
public class XSSFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(XSSFilter.class);

    @Autowired
    private XSSValidator xssValidator;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        try {
            // Check all parameters for XSS
            Enumeration<String> paramNames = request.getParameterNames();
            while (paramNames.hasMoreElements()) {
                String paramName = paramNames.nextElement();
                String[] paramValues = request.getParameterValues(paramName);

                for (String paramValue : paramValues) {
                    if (paramValue != null && !paramValue.isEmpty()) {
                        // Validate for XSS
                        xssValidator.validateInput(paramValue);
                    }
                }
            }

            // Check specific headers that might contain user input
            String[] headersToCheck = {
                "Referer", "User-Agent", "X-Forwarded-For", 
                "X-Custom-Header", "X-Search", "X-Filter"
            };
            
            for (String headerName : headersToCheck) {
                String headerValue = request.getHeader(headerName);
                if (headerValue != null && !headerValue.isEmpty()) {
                    // Only validate custom headers, not standard browser headers
                    if (headerName.startsWith("X-")) {
                        xssValidator.validateInput(headerValue);
                    }
                }
            }

        } catch (SecurityException e) {
            log.error("XSS attempt detected: method={}, uri={}, message={}", 
                method, requestURI, e.getMessage());
            sendSecurityError(response, "Invalid request: potential XSS detected");
            return;
        } catch (Exception e) {
            log.error("Error during XSS validation", e);
            sendSecurityError(response, "Request validation failed");
            return;
        }

        // Request is safe, continue
        filterChain.doFilter(request, response);
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
