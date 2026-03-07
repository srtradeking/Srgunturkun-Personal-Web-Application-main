package com.webapp.backend.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * JWT Utility Class
 * 
 * Handles JWT token generation, validation, and parsing.
 * Uses JJWT library for secure JWT operations.
 */
@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${JWT_SECRET:default-development-secret-key-32-chars-long-minimum-for-hmac-sha256}")
    private String jwtSecret;

    @Value("${JWT_EXPIRATION:86400000}") // 24 hours in milliseconds
    private long jwtExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    /**
     * Generate JWT token for a user
     */
    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, username);
    }

    /**
     * Generate JWT token with custom claims
     */
    public String generateToken(String username, Map<String, Object> extraClaims) {
        Map<String, Object> claims = new HashMap<>(extraClaims);
        return createToken(claims, username);
    }

    /**
     * Create JWT token with claims and subject
     */
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        // Add JWT ID (JTI) for token tracking
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .id(jti)  // Unique token identifier
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extract username from JWT token
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract expiration date from JWT token
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extract any claim from JWT token
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extract all claims from JWT token
     */
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            throw e;
        }
    }

    /**
     * Check if JWT token is expired
     */
    public Boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (JwtException e) {
            return true;
        }
    }

    /**
     * Validate JWT token
     */
    public Boolean validateToken(String token, String username) {
        try {
            final String extractedUsername = extractUsername(token);
            return (extractedUsername.equals(username) && !isTokenExpired(token));
        } catch (JwtException e) {
            return false;
        }
    }

    /**
     * Validate JWT token without username check
     */
    public Boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (JwtException e) {
            return false;
        }
    }

    /**
     * Get remaining time until token expires (in milliseconds)
     */
    public long getTokenExpirationTime(String token) {
        try {
            Date expiration = extractExpiration(token);
            return expiration.getTime() - new Date().getTime();
        } catch (JwtException e) {
            return 0;
        }
    }

    /**
     * Extract custom claim from token
     */
    public Object extractClaim(String token, String claimName) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.get(claimName);
        } catch (JwtException e) {
            return null;
        }
    }

    /**
     * Generate refresh token (longer expiration)
     */
    public String generateRefreshToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + (jwtExpiration * 7)); // 7 times longer than access token

        // Add JWT ID (JTI) for token tracking
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .id(jti)  // Unique token identifier
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Check if token is a refresh token
     */
    public Boolean isRefreshToken(String token) {
        try {
            Object type = extractClaim(token, "type");
            return "refresh".equals(type);
        } catch (JwtException e) {
            return false;
        }
    }

    /**
     * Extract JWT ID (JTI) from token
     */
    public String extractJwtId(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getId();
        } catch (JwtException e) {
            return null;
        }
    }

    /**
     * Extract issued at date from token
     */
    public Date extractIssuedAt(String token) {
        return extractClaim(token, Claims::getIssuedAt);
    }

    /**
     * Get token age in milliseconds
     */
    public long getTokenAge(String token) {
        try {
            Date issuedAt = extractIssuedAt(token);
            if (issuedAt == null) {
                return -1;
            }
            return System.currentTimeMillis() - issuedAt.getTime();
        } catch (JwtException e) {
            return -1;
        }
    }
}