package com.webapp.backend.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * File Inclusion Validator
 * 
 * Prevents Local File Inclusion (LFI) and Remote File Inclusion (RFI) attacks by:
 * - Validating file paths and URLs
 * - Blocking path traversal attempts
 * - Preventing access to sensitive files
 * - Blocking remote URL inclusion
 */
@Component
public class FileInclusionValidator {

    private static final Logger log = LoggerFactory.getLogger(FileInclusionValidator.class);

    // Sensitive file patterns to block
    private static final List<String> SENSITIVE_FILE_PATTERNS = Arrays.asList(
        "/etc/passwd", "/etc/shadow", "/etc/hosts", "/etc/group",
        "/.ssh/", "/.aws/", "/.env", "/proc/", "/sys/",
        "web.xml", "application.properties", "application.yml",
        "config.properties", ".git/", ".svn/",
        "WEB-INF/", "META-INF/", "classes/",
        "id_rsa", "id_dsa", "authorized_keys",
        "database.yml", "secrets.yml", "credentials"
    );

    // Dangerous file extensions
    private static final List<String> DANGEROUS_EXTENSIONS = Arrays.asList(
        "jsp", "jspx", "php", "phtml", "asp", "aspx",
        "config", "conf", "ini", "properties", "yml", "yaml",
        "xml", "log", "bak", "old", "tmp", "swp"
    );

    // URL schemes that could be used for RFI
    private static final List<String> BLOCKED_SCHEMES = Arrays.asList(
        "file", "ftp", "ftps", "gopher", "data", "jar", "mailto"
    );

    // Path traversal patterns
    private static final Pattern[] PATH_TRAVERSAL_PATTERNS = {
        Pattern.compile(".*\\.\\.[\\\\/].*"),              // ../
        Pattern.compile(".*[\\\\/]\\.\\..*"),              // /..
        Pattern.compile(".*%2e%2e[\\\\/].*", Pattern.CASE_INSENSITIVE),  // URL encoded
        Pattern.compile(".*%252e%252e[\\\\/].*", Pattern.CASE_INSENSITIVE),  // Double encoded
        Pattern.compile(".*\\.\\.\\/.*"),                  // ../
        Pattern.compile(".*\\.\\.\\\\.*"),                 // ..\
        Pattern.compile(".*%c0%ae%c0%ae.*", Pattern.CASE_INSENSITIVE),  // UTF-8 encoded
        Pattern.compile(".*%e0%80%ae%e0%80%ae.*", Pattern.CASE_INSENSITIVE)  // Overlong UTF-8
    };

    // Null byte patterns
    private static final Pattern NULL_BYTE_PATTERN = Pattern.compile(".*%00.*|.*\\x00.*");

    /**
     * Validate file path for LFI vulnerabilities
     */
    public void validateFilePath(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }

        String normalizedPath = filePath.toLowerCase().trim();

        // Check for null bytes
        if (NULL_BYTE_PATTERN.matcher(filePath).matches() || filePath.contains("\0")) {
            log.error("File inclusion attempt with null byte: {}", filePath);
            throw new SecurityException("Invalid file path: null byte detected");
        }

        // Check for path traversal
        for (Pattern pattern : PATH_TRAVERSAL_PATTERNS) {
            if (pattern.matcher(filePath).matches()) {
                log.error("Path traversal attempt detected: {}", filePath);
                throw new SecurityException("Invalid file path: path traversal detected");
            }
        }

        // Check for sensitive files
        for (String sensitive : SENSITIVE_FILE_PATTERNS) {
            if (normalizedPath.contains(sensitive.toLowerCase())) {
                log.error("Attempt to access sensitive file: {}", filePath);
                throw new SecurityException("Access to sensitive files is not allowed");
            }
        }

        // Check for absolute paths (should use relative paths)
        if (filePath.startsWith("/") || filePath.matches("^[a-zA-Z]:.*")) {
            log.warn("Absolute path detected: {}", filePath);
            // Don't block, but log for monitoring
        }

        // Check for dangerous extensions
        String extension = getFileExtension(filePath);
        if (DANGEROUS_EXTENSIONS.contains(extension.toLowerCase())) {
            log.warn("Dangerous file extension detected: {}", extension);
            // Don't block all, but log for monitoring
        }
    }

    /**
     * Validate URL for RFI vulnerabilities
     */
    public void validateUrl(String url) {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }

        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();

            // Block dangerous schemes
            if (scheme != null && BLOCKED_SCHEMES.contains(scheme.toLowerCase())) {
                log.error("Blocked URL scheme detected: {} in URL: {}", scheme, url);
                throw new SecurityException("URL scheme not allowed: " + scheme);
            }

            // Only allow http and https for external resources
            if (scheme != null && !scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) {
                log.error("Invalid URL scheme: {} in URL: {}", scheme, url);
                throw new SecurityException("Only HTTP and HTTPS URLs are allowed");
            }

            // Check for localhost/internal network access
            String host = uri.getHost();
            if (host != null) {
                String lowerHost = host.toLowerCase();
                if (lowerHost.equals("localhost") || 
                    lowerHost.equals("127.0.0.1") ||
                    lowerHost.equals("0.0.0.0") ||
                    lowerHost.startsWith("192.168.") ||
                    lowerHost.startsWith("10.") ||
                    lowerHost.startsWith("172.16.") ||
                    lowerHost.equals("::1") ||
                    lowerHost.equals("0:0:0:0:0:0:0:1")) {
                    log.error("Attempt to access internal network: {}", url);
                    throw new SecurityException("Access to internal network is not allowed");
                }
            }

            // Check for path traversal in URL path
            String path = uri.getPath();
            if (path != null) {
                validateFilePath(path);
            }

        } catch (URISyntaxException e) {
            log.error("Invalid URL syntax: {}", url, e);
            throw new SecurityException("Invalid URL format");
        }
    }

    /**
     * Validate resource key for cloud storage (S3/R2)
     */
    public void validateResourceKey(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Resource key cannot be null or empty");
        }

        // Check for path traversal
        if (key.contains("..")) {
            log.error("Path traversal in resource key: {}", key);
            throw new SecurityException("Invalid resource key: path traversal detected");
        }

        // Check for absolute paths
        if (key.startsWith("/")) {
            log.warn("Absolute path in resource key: {}", key);
            // Remove leading slash
            key = key.substring(1);
        }

        // Check for null bytes
        if (key.contains("\0")) {
            log.error("Null byte in resource key: {}", key);
            throw new SecurityException("Invalid resource key: null byte detected");
        }

        // Check for sensitive patterns
        String lowerKey = key.toLowerCase();
        if (lowerKey.contains("../") || lowerKey.contains("..\\") ||
            lowerKey.contains("/.") || lowerKey.contains("\\.")) {
            log.error("Suspicious pattern in resource key: {}", key);
            throw new SecurityException("Invalid resource key pattern");
        }
    }

    /**
     * Check if path is within allowed directory
     */
    public boolean isPathWithinDirectory(String path, String allowedDirectory) {
        if (path == null || allowedDirectory == null) {
            return false;
        }

        try {
            // Normalize paths
            String normalizedPath = normalizePath(path);
            String normalizedDir = normalizePath(allowedDirectory);

            // Check if path starts with allowed directory
            return normalizedPath.startsWith(normalizedDir);
        } catch (Exception e) {
            log.error("Error checking path containment", e);
            return false;
        }
    }

    /**
     * Normalize path by removing . and .. components
     */
    private String normalizePath(String path) {
        // Simple normalization - in production, use java.nio.file.Path
        return path.replaceAll("[\\\\/]+", "/")
                   .replaceAll("/\\./", "/")
                   .replaceAll("^\\./", "")
                   .replaceAll("/\\.$", "");
    }

    /**
     * Get file extension from path
     */
    private String getFileExtension(String path) {
        if (path == null || !path.contains(".")) {
            return "";
        }
        int lastDot = path.lastIndexOf('.');
        int lastSlash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        if (lastDot > lastSlash) {
            return path.substring(lastDot + 1);
        }
        return "";
    }

    /**
     * Sanitize file path for safe usage
     */
    public String sanitizeFilePath(String path) {
        if (path == null) {
            return null;
        }

        // Remove null bytes
        String sanitized = path.replace("\0", "");

        // Remove path traversal attempts
        sanitized = sanitized.replaceAll("\\.\\.", "");

        // Normalize slashes
        sanitized = sanitized.replaceAll("[\\\\/]+", "/");

        // Remove leading slashes
        sanitized = sanitized.replaceAll("^/+", "");

        return sanitized;
    }

    /**
     * Check if file extension is allowed for serving
     */
    public boolean isAllowedExtension(String filename, List<String> allowedExtensions) {
        if (filename == null || allowedExtensions == null) {
            return false;
        }

        String extension = getFileExtension(filename).toLowerCase();
        return allowedExtensions.contains(extension);
    }
}
