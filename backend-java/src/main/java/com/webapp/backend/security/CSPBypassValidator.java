package com.webapp.backend.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * CSP Bypass Validator
 * 
 * Validates content and configurations to prevent CSP bypass attacks:
 * - Detects unsafe CSP directives
 * - Validates nonce usage
 * - Checks for CSP bypass patterns
 * - Prevents base-uri manipulation
 */
@Component
public class CSPBypassValidator {

    private static final Logger log = LoggerFactory.getLogger(CSPBypassValidator.class);

    // Unsafe CSP directives that can lead to bypass
    private static final String[] UNSAFE_DIRECTIVES = {
        "'unsafe-inline'",
        "'unsafe-eval'",
        "data:",
        "*",
        "http:",
        "https://*"
    };

    // Patterns that indicate CSP bypass attempts
    private static final Pattern[] BYPASS_PATTERNS = {
        // JSONP endpoints
        Pattern.compile(".*\\.jsonp.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*callback=.*", Pattern.CASE_INSENSITIVE),
        
        // Angular template injection
        Pattern.compile(".*\\{\\{.*\\}\\}.*"),
        
        // Base tag manipulation
        Pattern.compile(".*<base[^>]*href.*", Pattern.CASE_INSENSITIVE),
        
        // Meta refresh
        Pattern.compile(".*<meta[^>]*http-equiv.*refresh.*", Pattern.CASE_INSENSITIVE),
        
        // Import statements
        Pattern.compile(".*@import.*", Pattern.CASE_INSENSITIVE),
        
        // Data URIs in CSS
        Pattern.compile(".*url\\s*\\(\\s*['\"]?data:.*", Pattern.CASE_INSENSITIVE)
    };

    /**
     * Validate CSP policy for unsafe directives
     */
    public void validateCSPPolicy(String cspPolicy) {
        if (cspPolicy == null || cspPolicy.isEmpty()) {
            throw new IllegalArgumentException("CSP policy cannot be null or empty");
        }

        String lowerPolicy = cspPolicy.toLowerCase();

        // Check for 'unsafe-inline' without nonce
        if (lowerPolicy.contains("'unsafe-inline'") && !lowerPolicy.contains("'nonce-")) {
            log.warn("CSP policy contains 'unsafe-inline' without nonce");
        }

        // Check for 'unsafe-eval'
        if (lowerPolicy.contains("'unsafe-eval'")) {
            log.warn("CSP policy contains 'unsafe-eval' - potential security risk");
        }

        // Check for wildcard sources
        if (lowerPolicy.matches(".*script-src[^;]*\\*.*")) {
            log.error("CSP policy contains wildcard in script-src");
            throw new SecurityException("Wildcard not allowed in script-src");
        }

        // Check for data: protocol in script-src
        if (lowerPolicy.matches(".*script-src[^;]*data:.*")) {
            log.error("CSP policy allows data: URIs in script-src");
            throw new SecurityException("data: URIs not allowed in script-src");
        }
    }

    /**
     * Validate content for CSP bypass patterns
     */
    public void validateContent(String content) {
        if (content == null || content.isEmpty()) {
            return;
        }

        for (Pattern pattern : BYPASS_PATTERNS) {
            if (pattern.matcher(content).find()) {
                log.error("CSP bypass pattern detected in content");
                throw new SecurityException("Content contains potential CSP bypass pattern");
            }
        }
    }

    /**
     * Validate base-uri to prevent base tag injection
     */
    public void validateBaseUri(String uri) {
        if (uri == null || uri.isEmpty()) {
            return;
        }

        // Base URI should only be 'self' or specific trusted origins
        if (!uri.equals("'self'") && !uri.startsWith("https://")) {
            log.error("Invalid base-uri: {}", uri);
            throw new SecurityException("Invalid base-uri value");
        }
    }

    /**
     * Check if URL is allowed by CSP
     */
    public boolean isUrlAllowedByCSP(String url, String cspDirective) {
        if (url == null || cspDirective == null) {
            return false;
        }

        String lowerUrl = url.toLowerCase();
        String lowerDirective = cspDirective.toLowerCase();

        // Check for data: URIs
        if (lowerUrl.startsWith("data:") && !lowerDirective.contains("data:")) {
            return false;
        }

        // Check for blob: URIs
        if (lowerUrl.startsWith("blob:") && !lowerDirective.contains("blob:")) {
            return false;
        }

        // Check for javascript: protocol
        if (lowerUrl.startsWith("javascript:")) {
            return false;
        }

        return true;
    }

    /**
     * Validate nonce in script tag
     */
    public boolean validateScriptNonce(String scriptTag, String expectedNonce) {
        if (scriptTag == null || expectedNonce == null) {
            return false;
        }

        // Check if script tag contains the expected nonce
        String noncePattern = "nonce=['\"]?" + Pattern.quote(expectedNonce) + "['\"]?";
        return Pattern.compile(noncePattern, Pattern.CASE_INSENSITIVE)
                     .matcher(scriptTag)
                     .find();
    }

    /**
     * Check for JSONP callback parameter (CSP bypass vector)
     */
    public boolean containsJSONPCallback(String url) {
        if (url == null) {
            return false;
        }

        String lowerUrl = url.toLowerCase();
        return lowerUrl.contains("callback=") || 
               lowerUrl.contains("jsonp=") ||
               lowerUrl.contains("cb=");
    }

    /**
     * Validate that CSP report-uri is properly configured
     */
    public void validateReportUri(String reportUri) {
        if (reportUri == null || reportUri.isEmpty()) {
            log.warn("CSP report-uri not configured");
            return;
        }

        // Report URI should be HTTPS in production
        if (!reportUri.startsWith("https://") && !reportUri.startsWith("/")) {
            log.warn("CSP report-uri should use HTTPS: {}", reportUri);
        }
    }

    /**
     * Check if CSP policy has proper frame-ancestors
     */
    public boolean hasProperFrameAncestors(String cspPolicy) {
        if (cspPolicy == null) {
            return false;
        }

        String lowerPolicy = cspPolicy.toLowerCase();
        
        // Should have frame-ancestors directive
        if (!lowerPolicy.contains("frame-ancestors")) {
            log.warn("CSP policy missing frame-ancestors directive");
            return false;
        }

        // Should not allow all origins
        if (lowerPolicy.matches(".*frame-ancestors.*\\*.*")) {
            log.error("CSP frame-ancestors allows all origins");
            return false;
        }

        return true;
    }

    /**
     * Validate form-action directive
     */
    public void validateFormAction(String formAction) {
        if (formAction == null || formAction.isEmpty()) {
            return;
        }

        // form-action should be restrictive
        if (formAction.equals("*") || formAction.contains("http:")) {
            log.error("Insecure form-action directive: {}", formAction);
            throw new SecurityException("form-action directive too permissive");
        }
    }

    /**
     * Check for CSP bypass via object-src
     */
    public boolean hasSecureObjectSrc(String cspPolicy) {
        if (cspPolicy == null) {
            return false;
        }

        String lowerPolicy = cspPolicy.toLowerCase();
        
        // object-src should be 'none' or very restrictive
        if (!lowerPolicy.contains("object-src")) {
            log.warn("CSP policy missing object-src directive");
            return false;
        }

        return lowerPolicy.contains("object-src 'none'") ||
               lowerPolicy.contains("object-src 'self'");
    }

    /**
     * Validate upgrade-insecure-requests directive
     */
    public boolean shouldUpgradeInsecureRequests(String cspPolicy) {
        if (cspPolicy == null) {
            return false;
        }

        return cspPolicy.toLowerCase().contains("upgrade-insecure-requests");
    }
}
