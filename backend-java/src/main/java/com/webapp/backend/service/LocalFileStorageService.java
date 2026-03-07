package com.webapp.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Local File Storage Service
 * Handles file storage on local filesystem
 */
@Slf4j
@Service
public class LocalFileStorageService {

    @Value("${file.storage.base-path:./uploads}")
    private String basePath;

    public String uploadFile(MultipartFile file, String filename) throws IOException {
        Path uploadPath = Paths.get(basePath);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        log.info("File uploaded locally: {}", filePath);
        return filename;
    }

    public void deleteFile(String filename) {
        try {
            Path filePath = Paths.get(basePath, filename);
            Files.deleteIfExists(filePath);
            log.info("File deleted locally: {}", filePath);
        } catch (IOException e) {
            log.error("Failed to delete file: {}", filename, e);
            throw new RuntimeException("Could not delete file", e);
        }
    }

    public InputStream downloadFile(String filename) throws IOException {
        Path filePath = Paths.get(basePath, filename);
        if (!Files.exists(filePath)) {
            return null;
        }
        return Files.newInputStream(filePath);
    }

    public boolean fileExists(String filename) {
        return Files.exists(Paths.get(basePath, filename));
    }

    public String generateSignedGetUrl(String filename, int expirySeconds) {
        // For local storage, we return direct URL via controller
        return null;
    }

    public String generateSignedPutUrl(String filename, int expirySeconds) {
        // For local storage, we handle uploads directly
        return null;
    }
}
