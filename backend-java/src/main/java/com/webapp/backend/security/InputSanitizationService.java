package com.webapp.backend.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Input Sanitization Service
 * 
 * Provides methods to sanitize and validate user inputs to prevent:
 * - Command Injection
 * - Path Traversal
 * - SQL Injection (additional layer)
 * - XSS (additional layer)
 */
@Service
public class InputSanitizationService {

    private static final Logger log = LoggerFactory.getLogger(InputSanitizationService.class);

    // Command injection patterns
    private static final Pattern[] COMMAND_INJECTION_PATTERNS = {
        Pattern.compile(".*[;&|`$(){}\\[\\]<>\\n\\r].*"),  // Shell metacharacters
        Pattern.compile(".*\\.\\.[\\\\/].*"),              // Path traversal
        Pattern.compile(".*(exec|eval|system|passthru|shell_exec|popen|proc_open).*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*(cmd|bash|sh|powershell|pwsh)\\.exe.*", Pattern.CASE_INSENSITIVE)
    };

    // Path traversal patterns
    private static final Pattern[] PATH_TRAVERSAL_PATTERNS = {
        Pattern.compile(".*\\.\\.[\\\\/].*"),              // ../ or ..\
        Pattern.compile(".*[\\\\/]\\.\\..*"),              // /.. or \..
        Pattern.compile(".*%2e%2e[\\\\/].*", Pattern.CASE_INSENSITIVE),  // URL encoded
        Pattern.compile(".*\\.\\.\\/.*"),                  // ../
        Pattern.compile(".*\\.\\.\\\\.*")                  // ..\
    };

    // Dangerous filename characters
    private static final Pattern DANGEROUS_FILENAME_PATTERN = 
        Pattern.compile(".*[;&|`$(){}\\[\\]<>\\n\\r\\t\\\\0].*");

    // SQL injection patterns (additional layer)
    private static final Pattern[] SQL_INJECTION_PATTERNS = {
        Pattern.compile(".*('|(--)|;|/\\*|\\*/|xp_|sp_|exec|execute|select|insert|update|delete|drop|create|alter|union).*", 
            Pattern.CASE_INSENSITIVE)
    };

    /**
     * Sanitize general text input
     */
    public String sanitizeText(String input) {
        if (input == null) {
            return null;
        }

        // Remove null bytes
        String sanitized = input.replace("\0", "");
        
        // Trim whitespace
        sanitized = sanitized.trim();
        
        // Remove control characters except newline and tab
        sanitized = sanitized.replaceAll("[\\p{Cntrl}&&[^\n\t]]", "");
        
        return sanitized;
    }

    /**
     * Sanitize filename - removes dangerous characters
     */
    public String sanitizeFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }

        // Remove path components
        String sanitized = filename.replaceAll(".*[\\\\/]", "");
        
        // Remove dangerous characters
        sanitized = sanitized.replaceAll("[^a-zA-Z0-9._-]", "_");
        
        // Remove leading dots (hidden files)
        sanitized = sanitized.replaceAll("^\\.+", "");
        
        // Limit length
        if (sanitized.length() > 255) {
            String extension = "";
            int dotIndex = sanitized.lastIndexOf('.');
            if (dotIndex > 0) {
                extension = sanitized.substring(dotIndex);
                sanitized = sanitized.substring(0, 255 - extension.length()) + extension;
            } else {
                sanitized = sanitized.substring(0, 255);
            }
        }

        if (sanitized.isEmpty()) {
            throw new IllegalArgumentException("Filename becomes empty after sanitization");
        }

        log.debug("Sanitized filename: {} -> {}", filename, sanitized);
        return sanitized;
    }

    /**
     * Validate input for command injection patterns
     */
    public void validateNoCommandInjection(String input) {
        if (input == null) {
            return;
        }

        for (Pattern pattern : COMMAND_INJECTION_PATTERNS) {
            if (pattern.matcher(input).matches()) {
                log.warn("Command injection attempt detected: {}", input);
                throw new SecurityException("Invalid input: potential command injection detected");
            }
        }
    }

    /**
     * Validate input for path traversal patterns
     */
    public void validateNoPathTraversal(String input) {
        if (input == null) {
            return;
        }

        for (Pattern pattern : PATH_TRAVERSAL_PATTERNS) {
            if (pattern.matcher(input).matches()) {
                log.warn("Path traversal attempt detected: {}", input);
                throw new SecurityException("Invalid input: potential path traversal detected");
            }
        }
    }

    /**
     * Validate filename is safe
     */
    public void validateFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }

        // Check for path traversal
        validateNoPathTraversal(filename);

        // Check for dangerous characters
        if (DANGEROUS_FILENAME_PATTERN.matcher(filename).matches()) {
            log.warn("Dangerous filename detected: {}", filename);
            throw new SecurityException("Invalid filename: contains dangerous characters");
        }

        // Check for null bytes
        if (filename.contains("\0")) {
            log.warn("Null byte in filename detected: {}", filename);
            throw new SecurityException("Invalid filename: contains null bytes");
        }

        // Check length
        if (filename.length() > 255) {
            throw new IllegalArgumentException("Filename too long (max 255 characters)");
        }
    }

    /**
     * Validate input for SQL injection patterns (additional layer)
     */
    public void validateNoSQLInjection(String input) {
        if (input == null) {
            return;
        }

        for (Pattern pattern : SQL_INJECTION_PATTERNS) {
            if (pattern.matcher(input).matches()) {
                log.warn("SQL injection attempt detected: {}", input);
                throw new SecurityException("Invalid input: potential SQL injection detected");
            }
        }
    }

    /**
     * Comprehensive validation for user input
     */
    public void validateUserInput(String input, InputType type) {
        if (input == null) {
            return;
        }

        switch (type) {
            case FILENAME:
                validateFilename(input);
                break;
            case PATH:
                validateNoPathTraversal(input);
                validateNoCommandInjection(input);
                break;
            case GENERAL:
                validateNoCommandInjection(input);
                break;
            case SQL_PARAM:
                validateNoSQLInjection(input);
                break;
            default:
                validateNoCommandInjection(input);
        }
    }

    /**
     * Check if string contains only alphanumeric characters
     */
    public boolean isAlphanumeric(String input) {
        return input != null && input.matches("^[a-zA-Z0-9]+$");
    }

    /**
     * Check if string is a valid identifier (alphanumeric + underscore)
     */
    public boolean isValidIdentifier(String input) {
        return input != null && input.matches("^[a-zA-Z_][a-zA-Z0-9_]*$");
    }

    /**
     * Sanitize and validate file extension
     */
    public String sanitizeFileExtension(String extension) {
        if (extension == null || extension.isEmpty()) {
            throw new IllegalArgumentException("Extension cannot be null or empty");
        }

        // Remove leading dot if present
        String sanitized = extension.startsWith(".") ? extension.substring(1) : extension;
        
        // Only allow alphanumeric characters
        sanitized = sanitized.toLowerCase().replaceAll("[^a-z0-9]", "");
        
        if (sanitized.isEmpty()) {
            throw new IllegalArgumentException("Invalid file extension");
        }

        return sanitized;
    }

    /**
     * Input types for validation
     */
    public enum InputType {
        FILENAME,
        PATH,
        GENERAL,
        SQL_PARAM
    }
}
