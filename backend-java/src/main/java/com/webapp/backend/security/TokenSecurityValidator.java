package com.webapp.backend.security;

import com.webapp.backend.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Token Security Validator
 * 
 * Provides comprehensive token security validation:
 * - Token blacklisting (for logout)
 * - Token reuse detection
 * - Token age validation
 * - Token binding validation
 */
@Component
public class TokenSecurityValidator {

    private static final Logger log = LoggerFactory.getLogger(TokenSecurityValidator.class);

    @Autowired
    private JwtUtil jwtUtil;

    // Blacklisted tokens (for logout)
    private final ConcurrentMap<String, Long> blacklistedTokens = new ConcurrentHashMap<>();

    // Token usage tracking (for reuse detection)
    private final ConcurrentMap<String, TokenUsage> tokenUsageMap = new ConcurrentHashMap<>();

    // Maximum token age before requiring refresh (in milliseconds)
    private static final long MAX_TOKEN_AGE = 24 * 60 * 60 * 1000; // 24 hours

    // Maximum allowed token reuse count
    private static final int MAX_REUSE_COUNT = 1000;

    /**
     * Validate token comprehensively
     */
    public boolean validateTokenSecurity(String token, String username) {
        if (token == null || token.isEmpty()) {
            log.warn("Empty token provided");
            return false;
        }

        // Check if token is blacklisted
        if (isTokenBlacklisted(token)) {
            log.warn("Blacklisted token used: user={}", username);
            return false;
        }

        // Validate JWT token
        if (!jwtUtil.validateToken(token, username)) {
            log.warn("Invalid JWT token for user: {}", username);
            return false;
        }

        // Check token age
        if (!isTokenAgeValid(token)) {
            log.warn("Token too old for user: {}", username);
            return false;
        }

        // Track token usage
        trackTokenUsage(token);

        // Check for suspicious reuse
        if (isSuspiciousReuse(token)) {
            log.error("Suspicious token reuse detected for user: {}", username);
            return false;
        }

        return true;
    }

    /**
     * Blacklist token (for logout)
     */
    public void blacklistToken(String token) {
        if (token == null || token.isEmpty()) {
            return;
        }

        try {
            // Get token expiration time
            long expirationTime = jwtUtil.extractExpiration(token).getTime();
            blacklistedTokens.put(token, expirationTime);
            
            log.info("Token blacklisted");
            
            // Clean up expired blacklisted tokens
            cleanupBlacklistedTokens();
        } catch (Exception e) {
            log.error("Error blacklisting token", e);
        }
    }

    /**
     * Check if token is blacklisted
     */
    public boolean isTokenBlacklisted(String token) {
        if (token == null) {
            return false;
        }

        Long expirationTime = blacklistedTokens.get(token);
        if (expirationTime == null) {
            return false;
        }

        // If token has expired, remove from blacklist
        if (expirationTime < System.currentTimeMillis()) {
            blacklistedTokens.remove(token);
            return false;
        }

        return true;
    }

    /**
     * Check if token age is valid
     */
    private boolean isTokenAgeValid(String token) {
        try {
            Date issuedAt = jwtUtil.extractClaim(token, claims -> claims.getIssuedAt());
            if (issuedAt == null) {
                return false;
            }

            long tokenAge = System.currentTimeMillis() - issuedAt.getTime();
            return tokenAge <= MAX_TOKEN_AGE;
        } catch (Exception e) {
            log.error("Error checking token age", e);
            return false;
        }
    }

    /**
     * Track token usage
     */
    private void trackTokenUsage(String token) {
        tokenUsageMap.compute(token, (key, usage) -> {
            if (usage == null) {
                return new TokenUsage(1, System.currentTimeMillis());
            } else {
                usage.incrementCount();
                usage.updateLastUsed();
                return usage;
            }
        });

        // Clean up old entries
        cleanupTokenUsageMap();
    }

    /**
     * Check for suspicious token reuse
     */
    private boolean isSuspiciousReuse(String token) {
        TokenUsage usage = tokenUsageMap.get(token);
        if (usage == null) {
            return false;
        }

        // Check if token is being reused too frequently
        if (usage.getCount() > MAX_REUSE_COUNT) {
            log.error("Token reused {} times", usage.getCount());
            return true;
        }

        return false;
    }

    /**
     * Clean up expired blacklisted tokens
     */
    private void cleanupBlacklistedTokens() {
        long now = System.currentTimeMillis();
        blacklistedTokens.entrySet().removeIf(entry -> entry.getValue() < now);
    }

    /**
     * Clean up old token usage entries
     */
    private void cleanupTokenUsageMap() {
        if (tokenUsageMap.size() > 10000) {
            long cutoffTime = System.currentTimeMillis() - MAX_TOKEN_AGE;
            tokenUsageMap.entrySet().removeIf(entry -> 
                entry.getValue().getLastUsed() < cutoffTime
            );
        }
    }

    /**
     * Validate token format
     */
    public boolean validateTokenFormat(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }

        // JWT tokens have 3 parts separated by dots
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            log.warn("Invalid JWT format: expected 3 parts, got {}", parts.length);
            return false;
        }

        // Check minimum length
        if (token.length() < 100) {
            log.warn("Token too short: {} characters", token.length());
            return false;
        }

        return true;
    }

    /**
     * Get blacklisted token count
     */
    public int getBlacklistedTokenCount() {
        return blacklistedTokens.size();
    }

    /**
     * Clear all blacklisted tokens (for testing/maintenance)
     */
    public void clearBlacklistedTokens() {
        blacklistedTokens.clear();
        log.info("All blacklisted tokens cleared");
    }

    /**
     * Token usage tracking class
     */
    private static class TokenUsage {
        private int count;
        private long lastUsed;

        public TokenUsage(int count, long lastUsed) {
            this.count = count;
            this.lastUsed = lastUsed;
        }

        public void incrementCount() {
            this.count++;
        }

        public void updateLastUsed() {
            this.lastUsed = System.currentTimeMillis();
        }

        public int getCount() {
            return count;
        }

        public long getLastUsed() {
            return lastUsed;
        }
    }
}
