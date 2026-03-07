package com.webapp.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * IP Address Controller
 * Provides endpoint for clients to retrieve their own IP address
 * Used for IP-based consent tracking and geolocation
 */
@Slf4j
@RestController
@RequestMapping("/ip-address")
public class IPAddressController {

    /**
     * Get client's IP address
     * Checks X-Forwarded-For header first (for proxied/load-balanced requests)
     * Falls back to remote address if not available
     * 
     * @param request HttpServletRequest containing IP information
     * @return ResponseEntity with ipAddress field
     */
    @GetMapping
    public ResponseEntity<Map<String, String>> getClientIP(HttpServletRequest request) {
        String ipAddress = extractClientIPAddress(request);
        Map<String, String> response = new HashMap<>();
        response.put("ipAddress", ipAddress);
        log.info("✅ Client IP retrieved: {}", ipAddress);
        return ResponseEntity.ok(response);
    }

    /**
     * Extract client IP address from request
     * Handles various proxy scenarios
     * 
     * Priority order:
     * 1. X-Forwarded-For (standard proxy header)
     * 2. Proxy-Client-IP (Apache)
     * 3. WL-Proxy-Client-IP (WebLogic)
     * 4. HTTP_CLIENT_IP (some proxies)
     * 5. HTTP_X_FORWARDED_FOR (alternative format)
     * 6. CF-Connecting-IP (Cloudflare)
     * 7. RemoteAddr (direct connection)
     * 
     * @param request HttpServletRequest to extract IP from
     * @return Client IP address as String
     */
    private String extractClientIPAddress(HttpServletRequest request) {
        // Check various proxy headers in order
        String[] ipHeaders = {
            "X-Forwarded-For",           // Standard proxy header
            "Proxy-Client-IP",           // Apache
            "WL-Proxy-Client-IP",        // WebLogic
            "HTTP_CLIENT_IP",            // Some proxies
            "HTTP_X_FORWARDED_FOR",      // Alternative format
            "CF-Connecting-IP"           // Cloudflare
        };

        for (String header : ipHeaders) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // If multiple IPs (X-Forwarded-For can have multiple), take the first one
                return ip.split(",")[0].trim();
            }
        }

        // Fallback to direct remote address
        String remoteAddr = request.getRemoteAddr();
        log.debug("📍 Using remote address: {}", remoteAddr);
        return remoteAddr;
    }
}
