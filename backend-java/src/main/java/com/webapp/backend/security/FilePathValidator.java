package com.webapp.backend.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * File Path Validator
 * 
 * Validates file paths to prevent:
 * - Path Traversal attacks
 * - Access to unauthorized directories
 * - Symbolic link attacks
 */
@Component
public class FilePathValidator {

    private static final Logger log = LoggerFactory.getLogger(FilePathValidator.class);

    // Allowed file extensions
    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = 
        Arrays.asList("jpg", "jpeg", "png", "gif", "webp", "bmp", "svg");
    
    private static final List<String> ALLOWED_VIDEO_EXTENSIONS = 
        Arrays.asList("mp4", "webm", "mov", "avi", "mkv", "flv", "wmv");
    
    private static final List<String> ALLOWED_DOCUMENT_EXTENSIONS = 
        Arrays.asList("pdf", "doc", "docx", "txt", "csv", "xls", "xlsx");

    // Blocked extensions (executable files)
    private static final List<String> BLOCKED_EXTENSIONS = Arrays.asList(
        "exe", "bat", "cmd", "com", "pif", "scr", "vbs", "js", "jar", 
        "sh", "bash", "ps1", "psm1", "dll", "so", "dylib", "app"
    );

    /**
     * Validate that a file path is safe and doesn't contain path traversal
     */
    public void validatePath(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }

        // Check for null bytes
        if (filePath.contains("\0")) {
            log.error("Null byte detected in file path: {}", filePath);
            throw new SecurityException("Invalid file path: contains null bytes");
        }

        // Check for path traversal patterns
        if (filePath.contains("..")) {
            log.error("Path traversal attempt detected: {}", filePath);
            throw new SecurityException("Invalid file path: path traversal detected");
        }

        // Check for absolute paths (should use relative paths)
        if (filePath.startsWith("/") || filePath.matches("^[a-zA-Z]:.*")) {
            log.warn("Absolute path detected: {}", filePath);
            // Note: This might be valid in some cases, log but don't block
        }

        // Check for URL-encoded path traversal
        String lowerPath = filePath.toLowerCase();
        if (lowerPath.contains("%2e%2e") || lowerPath.contains("%2f") || lowerPath.contains("%5c")) {
            log.error("URL-encoded path traversal attempt: {}", filePath);
            throw new SecurityException("Invalid file path: encoded path traversal detected");
        }
    }

    /**
     * Validate that a path is within an allowed base directory
     */
    public void validatePathWithinBase(String filePath, String baseDirectory) throws IOException {
        validatePath(filePath);

        Path base = Paths.get(baseDirectory).toRealPath();
        Path target = Paths.get(baseDirectory, filePath).normalize();

        // Resolve to canonical path to handle symlinks
        File targetFile = target.toFile();
        if (targetFile.exists()) {
            target = targetFile.getCanonicalFile().toPath();
        }

        if (!target.startsWith(base)) {
            log.error("Path traversal attempt: {} is outside base directory {}", target, base);
            throw new SecurityException("Invalid file path: outside allowed directory");
        }
    }

    /**
     * Validate file extension is allowed
     */
    public void validateFileExtension(String filename, FileType fileType) {
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }

        String extension = getFileExtension(filename).toLowerCase();

        // Check if extension is blocked
        if (BLOCKED_EXTENSIONS.contains(extension)) {
            log.error("Blocked file extension detected: {}", extension);
            throw new SecurityException("File type not allowed: " + extension);
        }

        // Check if extension matches expected file type
        List<String> allowedExtensions;
        switch (fileType) {
            case IMAGE:
                allowedExtensions = ALLOWED_IMAGE_EXTENSIONS;
                break;
            case VIDEO:
                allowedExtensions = ALLOWED_VIDEO_EXTENSIONS;
                break;
            case DOCUMENT:
                allowedExtensions = ALLOWED_DOCUMENT_EXTENSIONS;
                break;
            case ANY:
                // For ANY type, just check it's not blocked
                return;
            default:
                throw new IllegalArgumentException("Unknown file type: " + fileType);
        }

        if (!allowedExtensions.contains(extension)) {
            log.warn("File extension {} not allowed for type {}", extension, fileType);
            throw new IllegalArgumentException(
                String.format("File extension '%s' not allowed for %s files", extension, fileType)
            );
        }
    }

    /**
     * Validate filename doesn't contain dangerous characters
     */
    public void validateFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }

        // Check for path separators
        if (filename.contains("/") || filename.contains("\\")) {
            log.error("Path separator in filename: {}", filename);
            throw new SecurityException("Invalid filename: contains path separators");
        }

        // Check for null bytes
        if (filename.contains("\0")) {
            log.error("Null byte in filename: {}", filename);
            throw new SecurityException("Invalid filename: contains null bytes");
        }

        // Check for control characters
        if (filename.matches(".*[\\p{Cntrl}].*")) {
            log.error("Control characters in filename: {}", filename);
            throw new SecurityException("Invalid filename: contains control characters");
        }

        // Check for dangerous characters
        if (filename.matches(".*[;&|`$(){}\\[\\]<>].*")) {
            log.error("Dangerous characters in filename: {}", filename);
            throw new SecurityException("Invalid filename: contains dangerous characters");
        }

        // Check length
        if (filename.length() > 255) {
            throw new IllegalArgumentException("Filename too long (max 255 characters)");
        }
    }

    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    /**
     * Sanitize filename by removing dangerous characters
     */
    public String sanitizeFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }

        // Remove path components
        String sanitized = filename.replaceAll(".*[\\\\/]", "");
        
        // Remove dangerous characters, keep only safe ones
        sanitized = sanitized.replaceAll("[^a-zA-Z0-9._-]", "_");
        
        // Remove leading dots
        sanitized = sanitized.replaceAll("^\\.+", "");
        
        // Ensure not empty
        if (sanitized.isEmpty()) {
            sanitized = "file_" + System.currentTimeMillis();
        }

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

        return sanitized;
    }

    /**
     * File type enumeration
     */
    public enum FileType {
        IMAGE,
        VIDEO,
        DOCUMENT,
        ANY
    }
}
