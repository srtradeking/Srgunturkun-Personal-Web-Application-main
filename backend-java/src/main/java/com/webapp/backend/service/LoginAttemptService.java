package com.webapp.backend.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Service to track and manage login attempts to prevent brute force attacks.
 * 
 * Features:
 * - Tracks failed login attempts per IP address
 * - Blocks IP addresses after exceeding maximum attempts
 * - Auto-expires blocks after a configurable duration
 */
@Service
public class LoginAttemptService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);
    
    private static final int MAX_ATTEMPTS = 5;
    private static final int BLOCK_DURATION_MINUTES = 15;
    
    // Cache to store failed attempt counts per IP
    private final Cache<String, Integer> attemptsCache;
    
    // Cache to store blocked IPs
    private final Cache<String, Boolean> blockedCache;

    public LoginAttemptService() {
        // Failed attempts cache - expires after 15 minutes of inactivity
        this.attemptsCache = Caffeine.newBuilder()
                .expireAfterWrite(BLOCK_DURATION_MINUTES, TimeUnit.MINUTES)
                .maximumSize(10000)
                .build();
        
        // Blocked IPs cache - expires after 15 minutes
        this.blockedCache = Caffeine.newBuilder()
                .expireAfterWrite(BLOCK_DURATION_MINUTES, TimeUnit.MINUTES)
                .maximumSize(10000)
                .build();
    }

    /**
     * Record a failed login attempt for the given IP address.
     * If max attempts exceeded, block the IP.
     */
    public void loginFailed(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return;
        }
        
        Integer attempts = attemptsCache.getIfPresent(ipAddress);
        attempts = (attempts == null) ? 1 : attempts + 1;
        attemptsCache.put(ipAddress, attempts);
        
        log.warn("Failed login attempt from IP: {} (attempt {}/{})", ipAddress, attempts, MAX_ATTEMPTS);
        
        if (attempts >= MAX_ATTEMPTS) {
            blockedCache.put(ipAddress, true);
            log.error("IP address blocked due to excessive failed login attempts: {}", ipAddress);
        }
    }

    /**
     * Record a successful login and clear failed attempts for the IP.
     */
    public void loginSucceeded(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return;
        }
        
        attemptsCache.invalidate(ipAddress);
        blockedCache.invalidate(ipAddress);
        log.debug("Login succeeded for IP: {} - attempts cleared", ipAddress);
    }

    /**
     * Check if an IP address is currently blocked.
     */
    public boolean isBlocked(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return false;
        }
        
        Boolean blocked = blockedCache.getIfPresent(ipAddress);
        return blocked != null && blocked;
    }

    /**
     * Get the number of failed attempts for an IP address.
     */
    public int getFailedAttempts(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return 0;
        }
        
        Integer attempts = attemptsCache.getIfPresent(ipAddress);
        return attempts != null ? attempts : 0;
    }

    /**
     * Manually unblock an IP address.
     */
    public void unblockIp(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return;
        }
        
        attemptsCache.invalidate(ipAddress);
        blockedCache.invalidate(ipAddress);
        log.info("IP address manually unblocked: {}", ipAddress);
    }

    /**
     * Get remaining attempts before block.
     */
    public int getRemainingAttempts(String ipAddress) {
        int failed = getFailedAttempts(ipAddress);
        return Math.max(0, MAX_ATTEMPTS - failed);
    }
}
