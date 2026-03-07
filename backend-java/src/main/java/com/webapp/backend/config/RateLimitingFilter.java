package com.webapp.backend.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting filter to prevent brute force attacks and API abuse.
 * 
 * Implements token bucket algorithm using Bucket4j:
 * - General API: 100 requests per minute per IP
 * - Auth endpoints: 10 requests per minute per IP
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    // Store buckets per IP address
    private final Map<String, Bucket> generalBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> authBuckets = new ConcurrentHashMap<>();

    // Rate limits
    private static final int GENERAL_CAPACITY = 100;
    private static final int GENERAL_REFILL_TOKENS = 100;
    private static final Duration GENERAL_REFILL_DURATION = Duration.ofMinutes(1);

    private static final int AUTH_CAPACITY = 10;
    private static final int AUTH_REFILL_TOKENS = 10;
    private static final Duration AUTH_REFILL_DURATION = Duration.ofMinutes(1);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String ipAddress = getClientIpAddress(request);
        String requestURI = request.getRequestURI();

        // Determine if this is an auth endpoint
        boolean isAuthEndpoint = requestURI.contains("/auth/");

        // Get or create appropriate bucket
        Bucket bucket = isAuthEndpoint 
            ? resolveAuthBucket(ipAddress)
            : resolveGeneralBucket(ipAddress);

        // Try to consume a token
        if (bucket.tryConsume(1)) {
            // Token consumed successfully, proceed with request
            filterChain.doFilter(request, response);
        } else {
            // Rate limit exceeded
            log.warn("Rate limit exceeded for IP: {} on endpoint: {}", ipAddress, requestURI);
            response.setStatus(429); // Too Many Requests
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Too many requests. Please try again later.\"}");
        }
    }

    /**
     * Resolve or create a bucket for general API requests.
     */
    private Bucket resolveGeneralBucket(String ipAddress) {
        return generalBuckets.computeIfAbsent(ipAddress, key -> createGeneralBucket());
    }

    /**
     * Resolve or create a bucket for auth endpoints.
     */
    private Bucket resolveAuthBucket(String ipAddress) {
        return authBuckets.computeIfAbsent(ipAddress, key -> createAuthBucket());
    }

    /**
     * Create a bucket for general API requests.
     */
    private Bucket createGeneralBucket() {
        Bandwidth limit = Bandwidth.classic(GENERAL_CAPACITY, 
            Refill.intervally(GENERAL_REFILL_TOKENS, GENERAL_REFILL_DURATION));
        return Bucket.builder()
            .addLimit(limit)
            .build();
    }

    /**
     * Create a bucket for auth endpoints (stricter limits).
     */
    private Bucket createAuthBucket() {
        Bandwidth limit = Bandwidth.classic(AUTH_CAPACITY, 
            Refill.intervally(AUTH_REFILL_TOKENS, AUTH_REFILL_DURATION));
        return Bucket.builder()
            .addLimit(limit)
            .build();
    }

    /**
     * Extract client IP address from request, considering proxy headers.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * Clean up old buckets periodically to prevent memory leaks.
     * This is a simple implementation - in production, consider using a cache with TTL.
     */
    public void cleanupOldBuckets() {
        if (generalBuckets.size() > 10000) {
            generalBuckets.clear();
            log.info("Cleared general rate limit buckets");
        }
        if (authBuckets.size() > 10000) {
            authBuckets.clear();
            log.info("Cleared auth rate limit buckets");
        }
    }
}
