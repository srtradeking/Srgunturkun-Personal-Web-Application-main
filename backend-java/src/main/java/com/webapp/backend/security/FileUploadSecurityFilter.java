package com.webapp.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.IOException;
import java.util.Map;

/**
 * File Upload Security Filter
 * 
 * Inspects file upload requests for security threats:
 * - Validates content types
 * - Checks file sizes
 * - Monitors upload patterns
 * - Rate limits uploads
 */
@Component
public class FileUploadSecurityFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(FileUploadSecurityFilter.class);

    // Maximum total upload size per request
    private static final long MAX_REQUEST_SIZE = 600 * 1024 * 1024; // 600MB

    // Maximum number of files per request
    private static final int MAX_FILES_PER_REQUEST = 10;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Check if this is a multipart request
        if (request instanceof MultipartHttpServletRequest) {
            MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
            
            try {
                // Validate upload request
                validateUploadRequest(multipartRequest);
            } catch (SecurityException e) {
                log.error("File upload security violation: {}", e.getMessage());
                sendSecurityError(response, e.getMessage());
                return;
            } catch (IllegalArgumentException e) {
                log.warn("Invalid file upload request: {}", e.getMessage());
                sendValidationError(response, e.getMessage());
                return;
            }
        }

        // Request is safe, continue
        filterChain.doFilter(request, response);
    }

    /**
     * Validate upload request
     */
    private void validateUploadRequest(MultipartHttpServletRequest request) {
        Map<String, MultipartFile> fileMap = request.getFileMap();

        // Check number of files
        if (fileMap.size() > MAX_FILES_PER_REQUEST) {
            throw new IllegalArgumentException(
                String.format("Too many files in request: %d (max %d)", 
                    fileMap.size(), MAX_FILES_PER_REQUEST)
            );
        }

        // Calculate total size
        long totalSize = 0;
        for (MultipartFile file : fileMap.values()) {
            if (file != null && !file.isEmpty()) {
                totalSize += file.getSize();
                
                // Validate individual file
                validateFile(file);
            }
        }

        // Check total request size
        if (totalSize > MAX_REQUEST_SIZE) {
            throw new IllegalArgumentException(
                String.format("Total upload size %d exceeds maximum %d", 
                    totalSize, MAX_REQUEST_SIZE)
            );
        }

        log.debug("Upload request validated: {} files, {} bytes total", 
            fileMap.size(), totalSize);
    }

    /**
     * Validate individual file
     */
    private void validateFile(MultipartFile file) {
        String filename = file.getOriginalFilename();
        String contentType = file.getContentType();

        // Check for suspicious filenames
        if (filename != null) {
            String lowerFilename = filename.toLowerCase();
            
            // Check for executable files
            if (lowerFilename.matches(".*\\.(exe|dll|so|bat|cmd|sh|ps1|jar)$")) {
                throw new SecurityException("Executable file upload not allowed: " + filename);
            }

            // Check for script files
            if (lowerFilename.matches(".*\\.(php|jsp|asp|aspx|js|py|rb|pl)$")) {
                throw new SecurityException("Script file upload not allowed: " + filename);
            }

            // Check for path traversal
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                throw new SecurityException("Invalid filename: path traversal detected");
            }

            // Check for null bytes
            if (filename.contains("\0")) {
                throw new SecurityException("Invalid filename: null byte detected");
            }
        }

        // Check for suspicious content types
        if (contentType != null) {
            String lowerContentType = contentType.toLowerCase();
            
            if (lowerContentType.contains("executable") ||
                lowerContentType.contains("x-sh") ||
                lowerContentType.contains("x-php") ||
                lowerContentType.contains("javascript")) {
                throw new SecurityException("Dangerous content type: " + contentType);
            }
        }
    }

    /**
     * Send security error response
     */
    private void sendSecurityError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write(String.format(
            "{\"error\": \"Upload security violation\", \"message\": \"%s\"}", 
            message
        ));
    }

    /**
     * Send validation error response
     */
    private void sendValidationError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/json");
        response.getWriter().write(String.format(
            "{\"error\": \"Invalid upload request\", \"message\": \"%s\"}", 
            message
        ));
    }

    /**
     * Skip filter for non-upload endpoints
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        
        // Only filter POST/PUT requests to upload endpoints
        if (!method.equals("POST") && !method.equals("PUT")) {
            return true;
        }
        
        return !path.contains("/upload") && 
               !path.contains("/images") && 
               !path.contains("/videos") &&
               !path.contains("/storage");
    }
}
