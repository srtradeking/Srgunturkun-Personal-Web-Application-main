package com.webapp.backend.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

/**
 * Secure Token Generator
 * 
 * Generates cryptographically secure tokens for:
 * - Session IDs
 * - API keys
 * - Verification tokens
 * - Reset tokens
 * - CSRF tokens
 * 
 * Uses SecureRandom for cryptographic strength.
 */
@Component
public class SecureTokenGenerator {

    private static final Logger log = LoggerFactory.getLogger(SecureTokenGenerator.class);

    private final SecureRandom secureRandom;

    // Token lengths (in bytes)
    private static final int SESSION_TOKEN_LENGTH = 32;  // 256 bits
    private static final int API_KEY_LENGTH = 32;        // 256 bits
    private static final int VERIFICATION_TOKEN_LENGTH = 32;  // 256 bits
    private static final int SHORT_TOKEN_LENGTH = 16;    // 128 bits

    public SecureTokenGenerator() {
        this.secureRandom = new SecureRandom();
        // Force seeding for better randomness
        this.secureRandom.nextBytes(new byte[20]);
    }

    /**
     * Generate secure session token (256-bit)
     */
    public String generateSessionToken() {
        return generateToken(SESSION_TOKEN_LENGTH);
    }

    /**
     * Generate secure API key (256-bit)
     */
    public String generateApiKey() {
        return generateToken(API_KEY_LENGTH);
    }

    /**
     * Generate verification token (256-bit)
     */
    public String generateVerificationToken() {
        return generateToken(VERIFICATION_TOKEN_LENGTH);
    }

    /**
     * Generate short token (128-bit) for temporary use
     */
    public String generateShortToken() {
        return generateToken(SHORT_TOKEN_LENGTH);
    }

    /**
     * Generate token of specified length
     */
    public String generateToken(int lengthInBytes) {
        byte[] tokenBytes = new byte[lengthInBytes];
        secureRandom.nextBytes(tokenBytes);
        
        // Use URL-safe Base64 encoding
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        
        log.debug("Generated secure token of length: {} bytes", lengthInBytes);
        return token;
    }

    /**
     * Generate UUID-based token (128-bit)
     * Note: UUID v4 uses SecureRandom internally
     */
    public String generateUUIDToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generate alphanumeric token (for user-friendly codes)
     */
    public String generateAlphanumericToken(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder token = new StringBuilder(length);
        
        for (int i = 0; i < length; i++) {
            int index = secureRandom.nextInt(chars.length());
            token.append(chars.charAt(index));
        }
        
        return token.toString();
    }

    /**
     * Generate numeric token (for OTP, PIN)
     */
    public String generateNumericToken(int length) {
        StringBuilder token = new StringBuilder(length);
        
        for (int i = 0; i < length; i++) {
            token.append(secureRandom.nextInt(10));
        }
        
        return token.toString();
    }

    /**
     * Generate token with prefix (for identification)
     */
    public String generateTokenWithPrefix(String prefix, int lengthInBytes) {
        String token = generateToken(lengthInBytes);
        return prefix + "_" + token;
    }

    /**
     * Validate token format (basic check)
     */
    public boolean isValidTokenFormat(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }

        // Check minimum length (at least 128 bits = 22 base64 chars)
        if (token.length() < 22) {
            log.warn("Token too short: {} characters", token.length());
            return false;
        }

        // Check if it's valid Base64
        try {
            Base64.getUrlDecoder().decode(token);
            return true;
        } catch (IllegalArgumentException e) {
            log.warn("Invalid token format: not valid Base64");
            return false;
        }
    }

    /**
     * Get token entropy (bits)
     */
    public int getTokenEntropy(int lengthInBytes) {
        return lengthInBytes * 8;
    }

    /**
     * Generate secure random bytes
     */
    public byte[] generateRandomBytes(int length) {
        byte[] bytes = new byte[length];
        secureRandom.nextBytes(bytes);
        return bytes;
    }

    /**
     * Generate secure random integer
     */
    public int generateRandomInt(int bound) {
        return secureRandom.nextInt(bound);
    }

    /**
     * Generate secure random long
     */
    public long generateRandomLong() {
        return secureRandom.nextLong();
    }
}
