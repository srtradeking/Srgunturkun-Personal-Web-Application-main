package com.webapp.backend.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * Secure Resource Loader
 * 
 * Provides secure methods for loading files and resources with:
 * - Path validation
 * - Directory containment checks
 * - Extension whitelisting
 * - Access logging
 */
@Service
public class SecureResourceLoader {

    private static final Logger log = LoggerFactory.getLogger(SecureResourceLoader.class);

    @Autowired
    private FileInclusionValidator fileInclusionValidator;

    @Autowired
    private ResourceLoader resourceLoader;

    // Allowed resource directories (relative to application root)
    private static final List<String> ALLOWED_DIRECTORIES = Arrays.asList(
        "static/",
        "public/",
        "templates/",
        "resources/"
    );

    // Allowed file extensions for serving
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
        "html", "htm", "css", "js", "json",
        "jpg", "jpeg", "png", "gif", "webp", "svg", "ico",
        "pdf", "txt", "md"
    );

    /**
     * Load resource securely with validation
     */
    public Resource loadResource(String resourcePath) throws IOException {
        if (resourcePath == null || resourcePath.isEmpty()) {
            throw new IllegalArgumentException("Resource path cannot be null or empty");
        }

        // Validate for file inclusion attacks
        fileInclusionValidator.validateFilePath(resourcePath);

        // Check if path is within allowed directories
        if (!isPathAllowed(resourcePath)) {
            log.error("Attempt to access resource outside allowed directories: {}", resourcePath);
            throw new SecurityException("Access denied: resource not in allowed directory");
        }

        // Validate file extension
        if (!hasAllowedExtension(resourcePath)) {
            log.warn("Attempt to access resource with disallowed extension: {}", resourcePath);
            throw new SecurityException("Access denied: file type not allowed");
        }

        try {
            // Load resource using Spring's ResourceLoader
            Resource resource = resourceLoader.getResource("classpath:" + resourcePath);
            
            if (!resource.exists()) {
                log.warn("Resource not found: {}", resourcePath);
                throw new IOException("Resource not found: " + resourcePath);
            }

            if (!resource.isReadable()) {
                log.error("Resource not readable: {}", resourcePath);
                throw new IOException("Resource not readable: " + resourcePath);
            }

            log.debug("Resource loaded successfully: {}", resourcePath);
            return resource;

        } catch (Exception e) {
            log.error("Error loading resource: {}", resourcePath, e);
            throw new IOException("Failed to load resource", e);
        }
    }

    /**
     * Load file securely from filesystem
     */
    public InputStream loadFile(String basePath, String relativePath) throws IOException {
        if (basePath == null || relativePath == null) {
            throw new IllegalArgumentException("Base path and relative path cannot be null");
        }

        // Validate relative path
        fileInclusionValidator.validateFilePath(relativePath);

        // Sanitize paths
        String sanitizedRelative = fileInclusionValidator.sanitizeFilePath(relativePath);

        // Resolve full path
        Path base = Paths.get(basePath).toAbsolutePath().normalize();
        Path resolved = base.resolve(sanitizedRelative).normalize();

        // Ensure resolved path is within base directory
        if (!resolved.startsWith(base)) {
            log.error("Path traversal attempt: {} escapes base directory {}", relativePath, basePath);
            throw new SecurityException("Access denied: path traversal detected");
        }

        // Check if file exists
        if (!Files.exists(resolved)) {
            log.warn("File not found: {}", resolved);
            throw new IOException("File not found");
        }

        // Check if it's a regular file (not directory or symlink)
        if (!Files.isRegularFile(resolved)) {
            log.error("Attempt to access non-regular file: {}", resolved);
            throw new SecurityException("Access denied: not a regular file");
        }

        // Check file extension
        if (!hasAllowedExtension(resolved.toString())) {
            log.warn("Attempt to access file with disallowed extension: {}", resolved);
            throw new SecurityException("Access denied: file type not allowed");
        }

        log.info("File access granted: {}", resolved);
        return Files.newInputStream(resolved);
    }

    /**
     * Check if path is within allowed directories
     */
    private boolean isPathAllowed(String path) {
        String normalizedPath = path.replace("\\", "/");
        
        for (String allowedDir : ALLOWED_DIRECTORIES) {
            if (normalizedPath.startsWith(allowedDir)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Check if file has allowed extension
     */
    private boolean hasAllowedExtension(String path) {
        return fileInclusionValidator.isAllowedExtension(path, ALLOWED_EXTENSIONS);
    }

    /**
     * Validate and sanitize resource path
     */
    public String validateAndSanitizePath(String path) {
        if (path == null) {
            return null;
        }

        // Validate
        fileInclusionValidator.validateFilePath(path);

        // Sanitize
        return fileInclusionValidator.sanitizeFilePath(path);
    }

    /**
     * Check if resource exists safely
     */
    public boolean resourceExists(String resourcePath) {
        try {
            Resource resource = loadResource(resourcePath);
            return resource.exists();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get allowed extensions
     */
    public List<String> getAllowedExtensions() {
        return ALLOWED_EXTENSIONS;
    }

    /**
     * Get allowed directories
     */
    public List<String> getAllowedDirectories() {
        return ALLOWED_DIRECTORIES;
    }
}
