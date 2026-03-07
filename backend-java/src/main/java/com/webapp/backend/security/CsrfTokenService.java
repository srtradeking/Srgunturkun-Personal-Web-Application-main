package com.webapp.backend.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * CSRF Token Service
 * 
 * Generates and validates CSRF tokens for protecting against
 * Cross-Site Request Forgery attacks.
 * 
 * Uses double-submit cookie pattern with server-side validation.
 */
@Service
public class CsrfTokenService {

    private static final Logger log = LoggerFactory.getLogger(CsrfTokenService.class);
    
    private static final int TOKEN_LENGTH = 32; // 256 bits
    private static final int TOKEN_EXPIRY_MINUTES = 60;
    
    private final SecureRandom secureRandom;
    
    // Cache to store valid tokens with expiry
    private final Cache<String, TokenMetadata> tokenCache;

    public CsrfTokenService() {
        this.secureRandom = new SecureRandom();
        this.tokenCache = Caffeine.newBuilder()
                .expireAfterWrite(TOKEN_EXPIRY_MINUTES, TimeUnit.MINUTES)
                .maximumSize(100000)
                .build();
    }

    /**
     * Generate a new CSRF token for a user session
     */
    public String generateToken(String userId) {
        byte[] tokenBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        
        // Store token with metadata
        TokenMetadata metadata = new TokenMetadata(userId, System.currentTimeMillis());
        tokenCache.put(token, metadata);
        
        log.debug("Generated CSRF token for user: {}", userId);
        return token;
    }

    /**
     * Validate CSRF token
     */
    public boolean validateToken(String token, String userId) {
        if (token == null || token.isEmpty()) {
            log.warn("CSRF validation failed: token is null or empty");
            return false;
        }

        TokenMetadata metadata = tokenCache.getIfPresent(token);
        
        if (metadata == null) {
            log.warn("CSRF validation failed: token not found or expired for user: {}", userId);
            return false;
        }

        if (!metadata.userId.equals(userId)) {
            log.warn("CSRF validation failed: token user mismatch. Expected: {}, Got: {}", 
                metadata.userId, userId);
            return false;
        }

        // Check if token is expired (additional check beyond cache expiry)
        long tokenAge = System.currentTimeMillis() - metadata.createdAt;
        if (tokenAge > Duration.ofMinutes(TOKEN_EXPIRY_MINUTES).toMillis()) {
            log.warn("CSRF validation failed: token expired for user: {}", userId);
            tokenCache.invalidate(token);
            return false;
        }

        log.debug("CSRF token validated successfully for user: {}", userId);
        return true;
    }

    /**
     * Invalidate a CSRF token
     */
    public void invalidateToken(String token) {
        if (token != null) {
            tokenCache.invalidate(token);
            log.debug("CSRF token invalidated");
        }
    }

    /**
     * Invalidate all tokens for a user (e.g., on logout)
     */
    public void invalidateUserTokens(String userId) {
        tokenCache.asMap().entrySet().removeIf(entry -> 
            entry.getValue().userId.equals(userId)
        );
        log.debug("All CSRF tokens invalidated for user: {}", userId);
    }

    /**
     * Get token expiry time in milliseconds
     */
    public long getTokenExpiryMillis() {
        return Duration.ofMinutes(TOKEN_EXPIRY_MINUTES).toMillis();
    }

    /**
     * Token metadata for validation
     */
    private static class TokenMetadata {
        final String userId;
        final long createdAt;

        TokenMetadata(String userId, long createdAt) {
            this.userId = userId;
            this.createdAt = createdAt;
        }
    }
}
