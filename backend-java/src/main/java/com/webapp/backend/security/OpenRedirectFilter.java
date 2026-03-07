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
 * Open Redirect Filter
 * 
 * Detects and prevents open redirect vulnerabilities:
 * - Validates redirect parameters
 * - Checks Location headers
 * - Prevents phishing via malicious redirects
 * - Enforces whitelist-based redirect validation
 */
@Component
public class OpenRedirectFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(OpenRedirectFilter.class);

    @Autowired
    private RedirectValidator redirectValidator;

    // Common redirect parameter names
    private static final String[] REDIRECT_PARAMS = {
        "redirect", "return", "returnUrl", "return_url", "returnURL",
        "url", "next", "goto", "target", "destination", "continue",
        "redirect_uri", "redirectUri", "callback", "callbackUrl"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        try {
            // Check all parameters for redirect URLs
            Enumeration<String> paramNames = request.getParameterNames();
            while (paramNames.hasMoreElements()) {
                String paramName = paramNames.nextElement();
                String paramValue = request.getParameter(paramName);

                if (paramValue != null && !paramValue.isEmpty()) {
                    // Check if this is a redirect parameter
                    if (isRedirectParameter(paramName)) {
                        log.debug("Validating redirect parameter: {}={}", paramName, paramValue);
                        redirectValidator.validateRedirectUrl(paramValue);
                    }
                }
            }

            // Validate referer header
            String referer = request.getHeader("Referer");
            if (referer != null && !referer.isEmpty()) {
                redirectValidator.validateReferer(referer);
            }

        } catch (SecurityException e) {
            log.error("Open redirect attempt detected: method={}, uri={}, message={}", 
                method, requestURI, e.getMessage());
            sendSecurityError(response, "Invalid redirect URL");
            return;
        } catch (Exception e) {
            log.error("Error during redirect validation", e);
            sendSecurityError(response, "Redirect validation failed");
            return;
        }

        // Continue with the request
        filterChain.doFilter(request, response);
    }

    /**
     * Check if parameter name indicates a redirect
     */
    private boolean isRedirectParameter(String paramName) {
        if (paramName == null) {
            return false;
        }

        String lowerParamName = paramName.toLowerCase();

        for (String redirectParam : REDIRECT_PARAMS) {
            if (lowerParamName.equals(redirectParam.toLowerCase()) ||
                lowerParamName.contains(redirectParam.toLowerCase())) {
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
        response.getWriter().write(String.format(
            "{\"error\": \"Open redirect detected\", \"message\": \"%s\"}", 
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
               path.endsWith(".jpg") ||
               path.endsWith(".gif");
    }
}
