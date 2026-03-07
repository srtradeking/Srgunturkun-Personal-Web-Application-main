package com.webapp.backend.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Redirect Validator
 * 
 * Validates redirect URLs to prevent open redirect vulnerabilities:
 * - Whitelist-based validation
 * - Protocol validation (http/https only)
 * - Domain validation
 * - Path validation
 * - Prevents phishing attacks via redirects
 */
@Component
public class RedirectValidator {

    private static final Logger log = LoggerFactory.getLogger(RedirectValidator.class);

    @Value("${app.domain:localhost}")
    private String appDomain;

    @Value("${app.allowed-redirect-domains:}")
    private String allowedDomainsConfig;

    // Default allowed domains (same origin)
    private static final List<String> DEFAULT_ALLOWED_DOMAINS = Arrays.asList(
        "localhost",
        "127.0.0.1"
    );

    // Dangerous protocols
    private static final List<String> BLOCKED_PROTOCOLS = Arrays.asList(
        "javascript:",
        "data:",
        "vbscript:",
        "file:",
        "ftp:"
    );

    // Patterns for open redirect detection
    private static final Pattern[] REDIRECT_PATTERNS = {
        // Double slash (protocol-relative URL that could redirect anywhere)
        Pattern.compile("^//.*", Pattern.CASE_INSENSITIVE),
        
        // Backslash tricks
        Pattern.compile(".*\\\\.*", Pattern.CASE_INSENSITIVE),
        
        // URL-encoded slashes
        Pattern.compile(".*%2f%2f.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*%5c%5c.*", Pattern.CASE_INSENSITIVE),
        
        // Null bytes
        Pattern.compile(".*%00.*", Pattern.CASE_INSENSITIVE),
        
        // CRLF injection
        Pattern.compile(".*%0d.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*%0a.*", Pattern.CASE_INSENSITIVE),
        
        // @ symbol (username in URL)
        Pattern.compile(".*@.*", Pattern.CASE_INSENSITIVE)
    };

    /**
     * Validate redirect URL
     */
    public void validateRedirectUrl(String redirectUrl) {
        if (redirectUrl == null || redirectUrl.isEmpty()) {
            throw new IllegalArgumentException("Redirect URL cannot be null or empty");
        }

        String trimmedUrl = redirectUrl.trim();

        // Check for dangerous patterns
        for (Pattern pattern : REDIRECT_PATTERNS) {
            if (pattern.matcher(trimmedUrl).matches()) {
                log.error("Dangerous redirect pattern detected: {}", sanitizeForLog(trimmedUrl));
                throw new SecurityException("Invalid redirect URL: dangerous pattern detected");
            }
        }

        // Check for blocked protocols
        String lowerUrl = trimmedUrl.toLowerCase();
        for (String protocol : BLOCKED_PROTOCOLS) {
            if (lowerUrl.startsWith(protocol)) {
                log.error("Blocked protocol in redirect URL: {}", protocol);
                throw new SecurityException("Invalid redirect URL: dangerous protocol");
            }
        }

        // Parse and validate URL
        try {
            URI uri = new URI(trimmedUrl);

            // If it's a relative URL, it's safe (same origin)
            if (!uri.isAbsolute()) {
                validateRelativeUrl(trimmedUrl);
                return;
            }

            // Validate absolute URL
            validateAbsoluteUrl(uri);

        } catch (URISyntaxException e) {
            log.error("Invalid redirect URL syntax: {}", sanitizeForLog(trimmedUrl));
            throw new SecurityException("Invalid redirect URL: malformed URL");
        }
    }

    /**
     * Validate relative URL
     */
    private void validateRelativeUrl(String url) {
        // Relative URLs should start with / or be a path
        if (!url.startsWith("/") && !url.matches("^[a-zA-Z0-9_-]+.*")) {
            log.error("Invalid relative URL: {}", sanitizeForLog(url));
            throw new SecurityException("Invalid relative URL");
        }

        // Check for path traversal
        if (url.contains("..")) {
            log.error("Path traversal in redirect URL: {}", sanitizeForLog(url));
            throw new SecurityException("Invalid redirect URL: path traversal detected");
        }

        // Check for double slashes (could be protocol-relative)
        if (url.startsWith("//")) {
            log.error("Protocol-relative URL not allowed: {}", sanitizeForLog(url));
            throw new SecurityException("Invalid redirect URL: protocol-relative URLs not allowed");
        }
    }

    /**
     * Validate absolute URL
     */
    private void validateAbsoluteUrl(URI uri) {
        String scheme = uri.getScheme();
        String host = uri.getHost();

        // Only allow http and https
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            log.error("Invalid scheme in redirect URL: {}", scheme);
            throw new SecurityException("Invalid redirect URL: only http/https allowed");
        }

        // Validate host against whitelist
        if (host == null || !isHostAllowed(host)) {
            log.error("Redirect to non-whitelisted host: {}", host);
            throw new SecurityException("Invalid redirect URL: host not allowed");
        }

        // Check for suspicious ports
        int port = uri.getPort();
        if (port != -1 && !isPortAllowed(port)) {
            log.error("Redirect to suspicious port: {}", port);
            throw new SecurityException("Invalid redirect URL: port not allowed");
        }
    }

    /**
     * Check if host is allowed
     */
    private boolean isHostAllowed(String host) {
        if (host == null) {
            return false;
        }

        String lowerHost = host.toLowerCase();

        // Check if it's the app domain
        if (lowerHost.equals(appDomain.toLowerCase()) || 
            lowerHost.endsWith("." + appDomain.toLowerCase())) {
            return true;
        }

        // Check default allowed domains
        for (String allowedDomain : DEFAULT_ALLOWED_DOMAINS) {
            if (lowerHost.equals(allowedDomain) || 
                lowerHost.endsWith("." + allowedDomain)) {
                return true;
            }
        }

        // Check configured allowed domains
        if (allowedDomainsConfig != null && !allowedDomainsConfig.isEmpty()) {
            String[] configuredDomains = allowedDomainsConfig.split(",");
            for (String domain : configuredDomains) {
                String trimmedDomain = domain.trim().toLowerCase();
                if (lowerHost.equals(trimmedDomain) || 
                    lowerHost.endsWith("." + trimmedDomain)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Check if port is allowed
     */
    private boolean isPortAllowed(int port) {
        // Allow standard HTTP/HTTPS ports
        return port == 80 || port == 443 || port == 8080 || port == 3000;
    }

    /**
     * Sanitize redirect URL (make it safe)
     */
    public String sanitizeRedirectUrl(String redirectUrl) {
        if (redirectUrl == null || redirectUrl.isEmpty()) {
            return "/";
        }

        try {
            validateRedirectUrl(redirectUrl);
            return redirectUrl;
        } catch (SecurityException e) {
            log.warn("Unsafe redirect URL sanitized to /: {}", sanitizeForLog(redirectUrl));
            return "/";
        }
    }

    /**
     * Check if URL is safe for redirect
     */
    public boolean isSafeRedirectUrl(String redirectUrl) {
        if (redirectUrl == null || redirectUrl.isEmpty()) {
            return false;
        }

        try {
            validateRedirectUrl(redirectUrl);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get safe redirect URL or default
     */
    public String getSafeRedirectUrl(String redirectUrl, String defaultUrl) {
        if (isSafeRedirectUrl(redirectUrl)) {
            return redirectUrl;
        }
        return defaultUrl != null ? defaultUrl : "/";
    }

    /**
     * Validate redirect parameter name
     */
    public void validateRedirectParameter(String paramName, String paramValue) {
        if (paramName == null || paramValue == null) {
            return;
        }

        String lowerParamName = paramName.toLowerCase();

        // Common redirect parameter names
        if (lowerParamName.equals("redirect") ||
            lowerParamName.equals("return") ||
            lowerParamName.equals("returnurl") ||
            lowerParamName.equals("return_url") ||
            lowerParamName.equals("url") ||
            lowerParamName.equals("next") ||
            lowerParamName.equals("goto") ||
            lowerParamName.equals("target") ||
            lowerParamName.equals("destination") ||
            lowerParamName.equals("continue")) {
            
            validateRedirectUrl(paramValue);
        }
    }

    /**
     * Extract domain from URL
     */
    public String extractDomain(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }

        try {
            URI uri = new URI(url);
            return uri.getHost();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * Check if URL is external
     */
    public boolean isExternalUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }

        try {
            URI uri = new URI(url);
            
            // Relative URLs are not external
            if (!uri.isAbsolute()) {
                return false;
            }

            String host = uri.getHost();
            if (host == null) {
                return false;
            }

            // Check if it's the app domain
            return !host.equalsIgnoreCase(appDomain) && 
                   !host.endsWith("." + appDomain);

        } catch (URISyntaxException e) {
            return false;
        }
    }

    /**
     * Validate referer header
     */
    public void validateReferer(String referer) {
        if (referer == null || referer.isEmpty()) {
            return;
        }

        try {
            URI uri = new URI(referer);
            String host = uri.getHost();

            if (host != null && !isHostAllowed(host)) {
                log.warn("Suspicious referer from non-whitelisted host: {}", host);
            }
        } catch (URISyntaxException e) {
            log.warn("Invalid referer URL: {}", sanitizeForLog(referer));
        }
    }

    /**
     * Sanitize for logging
     */
    private String sanitizeForLog(String input) {
        if (input == null) {
            return "null";
        }

        if (input.length() > 100) {
            input = input.substring(0, 100) + "...";
        }

        return input.replaceAll("[\\r\\n\\t]", " ")
                   .replaceAll("[\\p{Cntrl}]", "");
    }

    /**
     * Build safe redirect URL
     */
    public String buildSafeRedirectUrl(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }

        // Ensure it starts with /
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        // Remove any protocol-relative indicators
        if (path.startsWith("//")) {
            path = path.substring(1);
        }

        return path;
    }
}
