package com.webapp.backend.config;

import com.webapp.backend.util.JsonUtil;
import com.webapp.backend.util.JwtUtil;
import com.webapp.backend.security.TokenSecurityValidator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import io.jsonwebtoken.Claims;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Collections;
import java.util.List;

/**
 * JWT Authentication Filter
 * 
 * Intercepts HTTP requests to validate JWT tokens and set authentication context.
 */
@Component
@Order(2)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TokenSecurityValidator tokenSecurityValidator;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        final String requestTokenHeader = request.getHeader("Authorization");
        String username = null;
        String jwtToken = null;

        log.debug("Processing request: {} {}", request.getMethod(), request.getRequestURI());
        log.debug("Authorization header present: {}", requestTokenHeader != null);

        // JWT Token is in the form "Bearer token". Remove Bearer word and get only the Token
        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7);
            log.debug("Extracted JWT token, length: {}", jwtToken.length());


            if (jwtToken != null) {
                try {
                    username = jwtUtil.extractUsername(jwtToken);
                    log.debug("Extracted username from HS256 token: {}", username);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid JWT claims: {}", e.getMessage());
                } catch (Exception e) {
                    log.warn("Error extracting username from JWT: {}", e.getMessage());
                }
            }
        } else {
            log.debug("No Authorization header or invalid format");
        }

        // Once we get the token validate it.
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            log.debug("Validating HS256 token for username: {}", username);
            
            // Check if token is blacklisted
            if (tokenSecurityValidator.isTokenBlacklisted(jwtToken)) {
                log.warn("Blacklisted token used by user: {}", username);
                chain.doFilter(request, response);
                return;
            }
            
            // Validate token
            if (jwtUtil.validateToken(jwtToken)) {
                // Extract role from token
                String role = (String) jwtUtil.extractClaim(jwtToken, "role");
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                
                if (role != null) {
                    // Ensure role starts with ROLE_ prefix if not already present
                    String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                    authorities.add(new SimpleGrantedAuthority(authority));
                    log.debug("Extracted role: {}", role);
                }

                // Create authentication token
                UsernamePasswordAuthenticationToken authToken = 
                    new UsernamePasswordAuthenticationToken(username, null, authorities);
                
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                // Set authentication in security context
                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.debug("Set authentication with username: {}", username);
            } else {
                log.warn("HS256 token validation failed");
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        
        // Skip JWT validation for these paths
        return path.startsWith("/api/auth/") || 
               path.startsWith("/api/health") || 
               path.startsWith("/api/swagger-ui") || 
               path.startsWith("/api/api-docs") || 
               path.startsWith("/api/h2-console") ||
               path.equals("/api/") ||
               path.equals("/api");
    }
}