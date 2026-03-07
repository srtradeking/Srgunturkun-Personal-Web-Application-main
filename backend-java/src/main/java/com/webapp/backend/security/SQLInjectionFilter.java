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
 * SQL Injection Filter
 * 
 * Inspects HTTP requests for SQL injection attempts in:
 * - Query parameters
 * - Request parameters
 * - Headers
 * - URI paths
 * 
 * This is a defense-in-depth measure. Primary protection comes from
 * using parameterized queries (JPA/PreparedStatements).
 */
@Component
public class SQLInjectionFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SQLInjectionFilter.class);

    @Autowired
    private SQLInjectionValidator sqlInjectionValidator;

    // Parameters that commonly contain search queries
    private static final String[] SEARCH_PARAMS = {
        "search", "query", "q", "name", "title", "keyword", "term"
    };

    // Parameters that should be numeric
    private static final String[] NUMERIC_PARAMS = {
        "id", "userId", "postId", "page", "size", "limit", "offset", "count"
    };

    // Parameters for sorting
    private static final String[] SORT_PARAMS = {
        "sort", "sortBy", "orderBy", "order"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String queryString = request.getQueryString();

        try {
            // Check query string for SQL injection
            if (queryString != null && !queryString.isEmpty()) {
                sqlInjectionValidator.validateInput(queryString);
            }

            // Check all parameters
            Enumeration<String> paramNames = request.getParameterNames();
            while (paramNames.hasMoreElements()) {
                String paramName = paramNames.nextElement();
                String[] paramValues = request.getParameterValues(paramName);

                for (String paramValue : paramValues) {
                    if (paramValue != null && !paramValue.isEmpty()) {
                        validateParameter(paramName, paramValue);
                    }
                }
            }

            // Check specific headers
            String[] headersToCheck = {"X-Search", "X-Filter", "X-Query"};
            for (String headerName : headersToCheck) {
                String headerValue = request.getHeader(headerName);
                if (headerValue != null && !headerValue.isEmpty()) {
                    sqlInjectionValidator.validateInput(headerValue);
                }
            }

        } catch (SecurityException e) {
            log.error("SQL injection attempt detected: method={}, uri={}, query={}", 
                request.getMethod(), requestURI, queryString);
            sendSecurityError(response, "Invalid request: potential SQL injection detected");
            return;
        } catch (IllegalArgumentException e) {
            log.warn("Invalid parameter in request: {}", e.getMessage());
            sendValidationError(response, e.getMessage());
            return;
        }

        // Request is safe, continue
        filterChain.doFilter(request, response);
    }

    /**
     * Validate parameter based on its name and expected type
     */
    private void validateParameter(String paramName, String paramValue) {
        String lowerParamName = paramName.toLowerCase();

        // Validate search parameters
        for (String searchParam : SEARCH_PARAMS) {
            if (lowerParamName.equals(searchParam) || lowerParamName.contains(searchParam)) {
                sqlInjectionValidator.validateSearchQuery(paramValue);
                return;
            }
        }

        // Validate numeric parameters
        for (String numericParam : NUMERIC_PARAMS) {
            if (lowerParamName.equals(numericParam) || lowerParamName.contains(numericParam)) {
                sqlInjectionValidator.validateNumericInput(paramValue);
                return;
            }
        }

        // Validate sort parameters
        for (String sortParam : SORT_PARAMS) {
            if (lowerParamName.equals(sortParam)) {
                sqlInjectionValidator.validateOrderByParameter(paramValue);
                return;
            }
        }

        // Default validation for other parameters
        sqlInjectionValidator.validateInput(paramValue);
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
     * Send validation error response
     */
    private void sendValidationError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/json");
        response.getWriter().write(String.format("{\"error\": \"Invalid parameter\", \"message\": \"%s\"}", message));
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
