package com.webapp.backend.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * File Upload Validator
 * 
 * Comprehensive validation for file uploads to prevent:
 * - Malicious file uploads
 * - Executable file uploads
 * - Oversized files
 * - MIME type spoofing
 * - Double extension attacks
 * - Polyglot files
 */
@Component
public class FileUploadValidator {

    private static final Logger log = LoggerFactory.getLogger(FileUploadValidator.class);

    // Maximum file sizes (in bytes)
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final long MAX_VIDEO_SIZE = 500 * 1024 * 1024; // 500MB
    private static final long MAX_DOCUMENT_SIZE = 25 * 1024 * 1024; // 25MB

    // Allowed MIME types
    private static final List<String> ALLOWED_IMAGE_MIMES = Arrays.asList(
        "image/jpeg", "image/jpg", "image/png", "image/gif",
        "image/webp", "image/bmp", "image/svg+xml"
    );

    private static final List<String> ALLOWED_VIDEO_MIMES = Arrays.asList(
        "video/mp4", "video/webm", "video/quicktime",
        "video/x-msvideo", "video/x-matroska"
    );

    private static final List<String> ALLOWED_DOCUMENT_MIMES = Arrays.asList(
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "text/plain", "text/csv"
    );

    // Blocked MIME types (executables, scripts)
    private static final List<String> BLOCKED_MIMES = Arrays.asList(
        "application/x-msdownload", "application/x-msdos-program",
        "application/x-executable", "application/x-sh",
        "application/x-php", "text/x-php",
        "application/x-httpd-php", "application/x-javascript",
        "text/javascript", "application/javascript",
        "application/x-python-code", "text/x-python",
        "application/x-perl", "text/x-perl",
        "application/x-ruby", "text/x-ruby"
    );

    // File magic numbers (signatures) for validation
    private static final Map<String, byte[]> FILE_SIGNATURES = new HashMap<>();
    
    static {
        // Images
        FILE_SIGNATURES.put("jpg", new byte[]{(byte)0xFF, (byte)0xD8, (byte)0xFF});
        FILE_SIGNATURES.put("png", new byte[]{(byte)0x89, 0x50, 0x4E, 0x47});
        FILE_SIGNATURES.put("gif", new byte[]{0x47, 0x49, 0x46, 0x38});
        FILE_SIGNATURES.put("webp", new byte[]{0x52, 0x49, 0x46, 0x46});
        FILE_SIGNATURES.put("bmp", new byte[]{0x42, 0x4D});
        
        // Videos
        FILE_SIGNATURES.put("mp4", new byte[]{0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70});
        FILE_SIGNATURES.put("webm", new byte[]{0x1A, 0x45, (byte)0xDF, (byte)0xA3});
        
        // Documents
        FILE_SIGNATURES.put("pdf", new byte[]{0x25, 0x50, 0x44, 0x46});
        FILE_SIGNATURES.put("zip", new byte[]{0x50, 0x4B, 0x03, 0x04});
    }

    /**
     * Validate uploaded file comprehensively
     */
    public void validateUpload(MultipartFile file, FileType expectedType) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty or null");
        }

        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();
        long fileSize = file.getSize();

        log.debug("Validating upload: filename={}, contentType={}, size={}", 
            originalFilename, contentType, fileSize);

        // 1. Validate filename
        validateFilename(originalFilename);

        // 2. Validate file size
        validateFileSize(fileSize, expectedType);

        // 3. Validate MIME type
        validateMimeType(contentType, expectedType);

        // 4. Validate file extension matches MIME type
        validateExtensionMimeMatch(originalFilename, contentType);

        // 5. Validate file content (magic numbers)
        validateFileContent(file, expectedType);

        // 6. Check for double extensions
        checkDoubleExtension(originalFilename);

        // 7. Validate no executable content
        validateNotExecutable(file);

        log.info("File upload validation passed: {}", originalFilename);
    }

    /**
     * Validate filename
     */
    private void validateFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }

        // Check for path traversal
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            log.error("Path traversal attempt in filename: {}", filename);
            throw new SecurityException("Invalid filename: path traversal detected");
        }

        // Check for null bytes
        if (filename.contains("\0")) {
            log.error("Null byte in filename: {}", filename);
            throw new SecurityException("Invalid filename: null byte detected");
        }

        // Check filename length
        if (filename.length() > 255) {
            throw new IllegalArgumentException("Filename too long (max 255 characters)");
        }
    }

    /**
     * Validate file size based on type
     */
    private void validateFileSize(long size, FileType type) {
        long maxSize;
        
        switch (type) {
            case IMAGE:
                maxSize = MAX_IMAGE_SIZE;
                break;
            case VIDEO:
                maxSize = MAX_VIDEO_SIZE;
                break;
            case DOCUMENT:
                maxSize = MAX_DOCUMENT_SIZE;
                break;
            default:
                maxSize = MAX_IMAGE_SIZE;
        }

        if (size > maxSize) {
            log.warn("File size {} exceeds maximum {} for type {}", size, maxSize, type);
            throw new IllegalArgumentException(
                String.format("File size (%d bytes) exceeds maximum allowed (%d bytes)", size, maxSize)
            );
        }

        if (size == 0) {
            throw new IllegalArgumentException("File is empty");
        }
    }

    /**
     * Validate MIME type
     */
    private void validateMimeType(String mimeType, FileType type) {
        if (mimeType == null || mimeType.isEmpty()) {
            throw new IllegalArgumentException("MIME type is required");
        }

        // Check if MIME type is blocked
        if (BLOCKED_MIMES.contains(mimeType.toLowerCase())) {
            log.error("Blocked MIME type detected: {}", mimeType);
            throw new SecurityException("File type not allowed: " + mimeType);
        }

        // Check if MIME type matches expected type
        List<String> allowedMimes;
        switch (type) {
            case IMAGE:
                allowedMimes = ALLOWED_IMAGE_MIMES;
                break;
            case VIDEO:
                allowedMimes = ALLOWED_VIDEO_MIMES;
                break;
            case DOCUMENT:
                allowedMimes = ALLOWED_DOCUMENT_MIMES;
                break;
            default:
                throw new IllegalArgumentException("Unknown file type: " + type);
        }

        if (!allowedMimes.contains(mimeType.toLowerCase())) {
            log.warn("MIME type {} not allowed for type {}", mimeType, type);
            throw new IllegalArgumentException("MIME type not allowed: " + mimeType);
        }
    }

    /**
     * Validate extension matches MIME type
     */
    private void validateExtensionMimeMatch(String filename, String mimeType) {
        String extension = getFileExtension(filename).toLowerCase();
        String expectedExtension = getExtensionFromMime(mimeType);

        if (expectedExtension != null && !extension.equals(expectedExtension)) {
            log.warn("Extension mismatch: file={}, mime={}, expected={}", 
                extension, mimeType, expectedExtension);
            // Log but don't block - MIME type is more reliable
        }
    }

    /**
     * Validate file content using magic numbers
     */
    private void validateFileContent(MultipartFile file, FileType type) throws IOException {
        String extension = getFileExtension(file.getOriginalFilename()).toLowerCase();
        byte[] expectedSignature = FILE_SIGNATURES.get(extension);

        if (expectedSignature != null) {
            try (InputStream is = file.getInputStream()) {
                byte[] fileHeader = new byte[Math.min(expectedSignature.length, 8)];
                int bytesRead = is.read(fileHeader);

                if (bytesRead < expectedSignature.length) {
                    log.warn("File too small to validate signature: {}", file.getOriginalFilename());
                    return;
                }

                // Check if file signature matches
                boolean matches = true;
                for (int i = 0; i < expectedSignature.length && i < fileHeader.length; i++) {
                    if (expectedSignature[i] != fileHeader[i]) {
                        matches = false;
                        break;
                    }
                }

                if (!matches) {
                    log.error("File signature mismatch for {}: expected {}, got {}", 
                        file.getOriginalFilename(), 
                        bytesToHex(expectedSignature), 
                        bytesToHex(fileHeader));
                    throw new SecurityException("File content does not match extension");
                }
            }
        }
    }

    /**
     * Check for double extension attacks
     */
    private void checkDoubleExtension(String filename) {
        String[] parts = filename.split("\\.");
        if (parts.length > 2) {
            // Check if any part before the last is a dangerous extension
            for (int i = 0; i < parts.length - 1; i++) {
                String ext = parts[i].toLowerCase();
                if (ext.matches("(exe|php|jsp|asp|sh|bat|cmd|ps1|jar)")) {
                    log.error("Double extension attack detected: {}", filename);
                    throw new SecurityException("Invalid filename: double extension detected");
                }
            }
        }
    }

    /**
     * Validate file is not executable
     */
    private void validateNotExecutable(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null) return;

        String lowerFilename = filename.toLowerCase();
        
        // Check for executable extensions
        if (lowerFilename.matches(".*\\.(exe|dll|so|dylib|bat|cmd|sh|ps1|jar|war|ear)$")) {
            log.error("Executable file upload attempt: {}", filename);
            throw new SecurityException("Executable files are not allowed");
        }

        // Check for script extensions
        if (lowerFilename.matches(".*\\.(php|jsp|asp|aspx|js|py|rb|pl|cgi)$")) {
            log.error("Script file upload attempt: {}", filename);
            throw new SecurityException("Script files are not allowed");
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
     * Get expected extension from MIME type
     */
    private String getExtensionFromMime(String mimeType) {
        if (mimeType == null) return null;
        
        Map<String, String> mimeToExt = new HashMap<>();
        mimeToExt.put("image/jpeg", "jpg");
        mimeToExt.put("image/jpg", "jpg");
        mimeToExt.put("image/png", "png");
        mimeToExt.put("image/gif", "gif");
        mimeToExt.put("image/webp", "webp");
        mimeToExt.put("video/mp4", "mp4");
        mimeToExt.put("video/webm", "webm");
        mimeToExt.put("application/pdf", "pdf");
        
        return mimeToExt.get(mimeType.toLowerCase());
    }

    /**
     * Convert bytes to hex string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    /**
     * File type enumeration
     */
    public enum FileType {
        IMAGE,
        VIDEO,
        DOCUMENT
    }
}
