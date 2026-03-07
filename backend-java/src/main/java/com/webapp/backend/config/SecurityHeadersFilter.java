package com.webapp.backend.config;

import com.webapp.backend.security.CSPNonceGenerator;
import com.webapp.backend.security.CSPBypassValidator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Security Headers Filter
 * 
 * Adds comprehensive security headers to protect against:
 * - XSS (Cross-Site Scripting)
 * - Clickjacking
 * - MIME-type sniffing
 * - Cross-origin attacks
 */
@Component
@Order(1)
public class SecurityHeadersFilter extends HttpFilter {

    private static final Logger log = LoggerFactory.getLogger(SecurityHeadersFilter.class);

    @Autowired
    private CSPNonceGenerator cspNonceGenerator;

    @Autowired
    private CSPBypassValidator cspBypassValidator;

    @Value("${csp.report-uri:/api/csp-report}")
    private String cspReportUri;

    @Value("${csp.strict-mode:true}")
    private boolean strictMode;

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        
        // XSS Protection
        // Enable browser's XSS filter (legacy browsers)
        res.setHeader("X-XSS-Protection", "1; mode=block");
        
        // Content Security Policy (CSP)
        // Generate nonce for inline scripts/styles
        String nonce = cspNonceGenerator.generateNonce();
        req.setAttribute("cspNonce", nonce);
        
        // Build strict CSP policy
        String csp;
        if (strictMode) {
            // Strict mode: No unsafe-inline, no unsafe-eval, nonce-based
            csp = "default-src 'self'; " +
                  "script-src 'self' 'nonce-" + nonce + "'; " +
                  "style-src 'self' 'nonce-" + nonce + "'; " +
                  "img-src 'self' data: https:; " +
                  "font-src 'self' data:; " +
                  "connect-src 'self'; " +
                  "frame-src 'none'; " +
                  "frame-ancestors 'none'; " +
                  "base-uri 'self'; " +
                  "form-action 'self'; " +
                  "object-src 'none'; " +
                  "media-src 'self' blob: data:; " +
                  "worker-src 'self'; " +
                  "manifest-src 'self'; " +
                  "upgrade-insecure-requests; " +
                  "block-all-mixed-content; " +
                  "report-uri " + cspReportUri;
        } else {
            // Relaxed mode for development (still secure)
            csp = "default-src 'self'; " +
                  "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                  "style-src 'self' 'unsafe-inline'; " +
                  "img-src 'self' data: https:; " +
                  "font-src 'self' data:; " +
                  "connect-src 'self'; " +
                  "frame-ancestors 'self'; " +
                  "base-uri 'self'; " +
                  "form-action 'self'; " +
                  "object-src 'none'; " +
                  "media-src 'self' blob: data:; " +
                  "report-uri " + cspReportUri;
        }
        
        // Validate CSP policy
        try {
            cspBypassValidator.validateCSPPolicy(csp);
        } catch (SecurityException e) {
            log.error("CSP policy validation failed: {}", e.getMessage());
        }
        
        res.setHeader("Content-Security-Policy", csp);
        
        // Also set report-only header for monitoring
        res.setHeader("Content-Security-Policy-Report-Only", csp);
        
        // Prevent MIME-type sniffing
        res.setHeader("X-Content-Type-Options", "nosniff");
        
        // Clickjacking protection (redundant with CSP frame-ancestors, but kept for older browsers)
        res.setHeader("X-Frame-Options", "DENY");
        
        // Referrer Policy
        res.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        
        // Permissions Policy (formerly Feature Policy)
        res.setHeader("Permissions-Policy", 
            "geolocation=(), microphone=(), camera=(), payment=()");
        
        // Cross-Origin policies - Relaxed for media playback compatibility
        res.setHeader("Cross-Origin-Opener-Policy", "same-origin-allow-popups");
        // Allow resources to be requested by other origins (needed for some media scenarios)
        res.setHeader("Cross-Origin-Resource-Policy", "cross-origin");
        // Disable strict COEP to allow loading non-CORP resources
        // res.setHeader("Cross-Origin-Embedder-Policy", "require-corp"); 
        
        // Additional security headers
        res.setHeader("X-Permitted-Cross-Domain-Policies", "none");
        res.setHeader("X-Download-Options", "noopen");
        res.setHeader("X-DNS-Prefetch-Control", "off");
        
        // Clear potentially dangerous headers
        res.setHeader("X-Powered-By", "");
        res.setHeader("Server", "");
        
        // Strict Transport Security (HTTPS only)
        // Uncomment in production with HTTPS
        // res.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");
        
        log.debug("Security headers applied for: {}", req.getRequestURI());
        
        // Continue the filter chain
        chain.doFilter(req, res);
    }
}
