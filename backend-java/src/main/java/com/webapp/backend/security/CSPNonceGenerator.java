package com.webapp.backend.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * CSP Nonce Generator
 * 
 * Generates cryptographically secure nonces for Content Security Policy.
 * Nonces are used to allow specific inline scripts and styles while
 * blocking all other inline content, preventing CSP bypass attacks.
 */
@Component
public class CSPNonceGenerator {

    private static final Logger log = LoggerFactory.getLogger(CSPNonceGenerator.class);

    @Autowired
    private SecureTokenGenerator tokenGenerator;

    private static final int NONCE_LENGTH = 16; // 128 bits

    /**
     * Generate a cryptographically secure nonce for CSP
     */
    public String generateNonce() {
        byte[] nonceBytes = tokenGenerator.generateRandomBytes(NONCE_LENGTH);
        String nonce = Base64.getEncoder().encodeToString(nonceBytes);
        
        log.debug("Generated CSP nonce");
        return nonce;
    }

    /**
     * Generate nonce with prefix for identification
     */
    public String generateNonceWithPrefix(String prefix) {
        String nonce = generateNonce();
        return prefix + "-" + nonce;
    }

    /**
     * Validate nonce format
     */
    public boolean isValidNonce(String nonce) {
        if (nonce == null || nonce.isEmpty()) {
            return false;
        }

        // Check minimum length
        if (nonce.length() < 22) { // Base64 encoded 16 bytes
            return false;
        }

        // Check if it's valid Base64
        try {
            Base64.getDecoder().decode(nonce);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
