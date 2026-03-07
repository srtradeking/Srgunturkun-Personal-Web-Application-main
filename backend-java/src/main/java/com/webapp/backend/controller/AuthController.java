package com.webapp.backend.controller;

import com.webapp.backend.dto.AuthRequestDTO;
import com.webapp.backend.dto.AuthResponseDTO;
import com.webapp.backend.dto.RefreshTokenRequestDTO;
import com.webapp.backend.dto.RegisterRequestDTO;
import com.webapp.backend.model.User;
import com.webapp.backend.model.UserProfile;
import com.webapp.backend.repository.UserProfileRepository;
import com.webapp.backend.repository.UserRepository;
import com.webapp.backend.service.EmailVerificationService;
import com.webapp.backend.service.LoginAttemptService;
import com.webapp.backend.security.CsrfTokenService;
import com.webapp.backend.security.TokenSecurityValidator;
import com.webapp.backend.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * Authentication Controller
 * 
 * Handles user authentication, token generation, and token refresh.
 */
@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailVerificationService emailVerificationService;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Autowired
    private CsrfTokenService csrfTokenService;

    @Autowired
    private TokenSecurityValidator tokenSecurityValidator;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequestDTO authRequest, HttpServletRequest request) {
        try {
            String email = authRequest.getEmail();
            String rawPassword = authRequest.getPassword();
            String ipAddress = getClientIpAddress(request);

            log.info("Login attempt for email={} from IP={}", email, ipAddress);

            // Check if IP is blocked due to too many failed attempts
            if (loginAttemptService.isBlocked(ipAddress)) {
                log.warn("Login blocked for IP={} due to excessive failed attempts", ipAddress);
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(Map.of("error", "Too many failed login attempts. Please try again later."));
            }

            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                log.warn("Login failed: user not found for email={}", email);
                loginAttemptService.loginFailed(ipAddress);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid email or password"));
            }

            if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
                log.warn("Login failed: invalid password for email={}", email);
                loginAttemptService.loginFailed(ipAddress);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid email or password"));
            }

            UserProfile profile = user.getUserProfile();
            if (profile != null && Boolean.FALSE.equals(profile.getIsVerified())) {
                log.warn("Login failed: email not verified for email={}", email);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "EMAIL_NOT_VERIFIED"));
            }
            Long userId = profile != null ? profile.getId() : null;
            String username = profile != null ? profile.getUsername() : email;
            String displayName = profile != null ? profile.getDisplayName() : null;
            String role = user.getRole().name();

            Map<String, Object> claims = new HashMap<>();
            if (userId != null) {
                claims.put("userId", userId);
            }
            claims.put("email", email);
            if (displayName != null) {
                claims.put("displayName", displayName);
            }
            claims.put("role", role);

            String accessToken = jwtUtil.generateToken(username, claims);
            String refreshToken = jwtUtil.generateRefreshToken(username);

            AuthResponseDTO response = AuthResponseDTO.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .username(username)
                    .email(email)
                    .displayName(displayName)
                    .role(role)
                    .userId(userId)
                    .build();

            // Clear failed attempts on successful login
            loginAttemptService.loginSucceeded(ipAddress);
            
            // Generate CSRF token for the user
            String csrfToken = csrfTokenService.generateToken(username);
            
            log.info("User logged in successfully: email={}, userId={}", email, userId);
            
            // Add CSRF token to response headers
            return ResponseEntity.ok()
                .header("X-CSRF-Token", csrfToken)
                .body(response);

        } catch (Exception e) {
            log.error("Authentication error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Authentication failed"));
        }
    }

    /**
     * Register a new user with email, username, and password.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequestDTO request) {
        try {
            String email = request.getEmail();
            String username = request.getUsername();

            if (userRepository.findByEmail(email).isPresent()) {
                log.warn("Registration failed: email already in use: {}", email);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Email is already registered"));
            }

            if (userProfileRepository.findByUsername(username).isPresent()) {
                log.warn("Registration failed: username already in use: {}", username);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Username is already taken"));
            }

            // Create profile first
            UserProfile profile = UserProfile.builder()
                    .username(username)
                    .email(email)
                    .displayName(username)
                    .isActive(true)
                    .isVerified(false)
                    .isBanned(false)
                    .build();
            profile = userProfileRepository.save(profile);

            // Hash password
            String passwordHash = passwordEncoder.encode(request.getPassword());

            // Create user credentials linked to profile
            User user = User.builder()
                    .email(email)
                    .passwordHash(passwordHash)
                    .userProfile(profile)
                    .build();
            userRepository.save(user);

            try {
                emailVerificationService.sendVerificationEmail(profile);
            } catch (Exception ex) {
                log.error("Error sending verification email for email={}", email, ex);
            }

            log.info("User registered successfully (verification email sent): email={}, userProfileId={}", email, profile.getId());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "User registered successfully. Please verify your email before logging in."));

        } catch (Exception e) {
            log.error("Registration error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Registration failed"));
        }
    }

    /**
     * Verify email using token from verification link
     */
    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam("token") String token) {
        try {
            boolean ok = emailVerificationService.verifyToken(token);
            if (ok) {
                return ResponseEntity.ok(Map.of("status", "ok"));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", "error", "error", "INVALID_OR_EXPIRED_TOKEN"));
        } catch (Exception e) {
            log.error("Email verification error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "error", "VERIFICATION_FAILED"));
        }
    }

    /**
     * Resend verification email for a given address
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@RequestBody Map<String, String> body) {
        try {
            String email = body.get("email");
            if (email == null || email.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Email is required"));
            }

            emailVerificationService.resendVerification(email);
            return ResponseEntity.ok(Map.of("message", "If the account exists and is not verified, a verification email has been sent."));
        } catch (Exception e) {
            log.error("Resend verification error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Resend verification failed"));
        }
    }

    /**
     * Refresh JWT access token using refresh token
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequestDTO refreshRequest) {
        try {
            String refreshToken = refreshRequest.getRefreshToken();
            
            // Validate refresh token
            if (jwtUtil.validateToken(refreshToken) && jwtUtil.isRefreshToken(refreshToken)) {
                String username = jwtUtil.extractUsername(refreshToken);
                
                // Generate new access token
                String newAccessToken = jwtUtil.generateToken(username);
                
                AuthResponseDTO response = new AuthResponseDTO();
                response.setAccessToken(newAccessToken);
                response.setRefreshToken(refreshToken); // Keep the same refresh token
                response.setUsername(username);
                response.setUserId(null); // Not available in this flow
                
                log.info("Token refreshed for user: {}", username);
                return ResponseEntity.ok(response);
            } else {
                log.warn("Invalid or expired refresh token");
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid refresh token"));
            }
            
        } catch (Exception e) {
            log.error("Token refresh error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Token refresh failed"));
        }
    }

    /**
     * Validate current JWT token
     */
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                
                if (jwtUtil.validateToken(token)) {
                    String username = jwtUtil.extractUsername(token);
                    long expirationTime = jwtUtil.getTokenExpirationTime(token);
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("valid", true);
                    response.put("username", username);
                    response.put("expiresIn", expirationTime);
                    
                    return ResponseEntity.ok(response);
                }
            }
            
            return ResponseEntity.badRequest()
                .body(Map.of("valid", false, "error", "Invalid token"));
                
        } catch (Exception e) {
            log.error("Token validation error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("valid", false, "error", "Token validation failed"));
        }
    }

    /**
     * Logout user (server-side token invalidation)
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            Authentication authentication) {
        
        // Blacklist the JWT token
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            tokenSecurityValidator.blacklistToken(token);
            log.info("JWT token blacklisted");
        }
        
        // Invalidate CSRF tokens for the user
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            csrfTokenService.invalidateUserTokens(username);
            log.info("CSRF tokens invalidated for user: {}", username);
        }
        
        SecurityContextHolder.clearContext();
        
        log.info("User logged out successfully");
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    /**
     * Get current user info
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("username", username);
            userInfo.put("authenticated", true);
            
            return ResponseEntity.ok(userInfo);
        }
        
        return ResponseEntity.badRequest()
            .body(Map.of("error", "User not authenticated"));
    }

    /**
     * Extract client IP address from request, considering proxy headers.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

}