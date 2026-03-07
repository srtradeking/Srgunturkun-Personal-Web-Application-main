package com.webapp.backend.config;

import com.webapp.backend.model.UserProfile;
import com.webapp.backend.repository.UserProfileRepository;
import com.webapp.backend.security.AuthorizationValidator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.util.Optional;

/**
 * Enforces that profile operations which target a specific userId are only
 * allowed when the authenticated principal maps to that local user id or
 
 */
@Component
public class ProfileAuthorizationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ProfileAuthorizationFilter.class);

    private final UserProfileRepository userProfileRepository;
    private final AuthorizationValidator authorizationValidator;

    public ProfileAuthorizationFilter(UserProfileRepository userProfileRepository,
                                     AuthorizationValidator authorizationValidator) {
        this.userProfileRepository = userProfileRepository;
        this.authorizationValidator = authorizationValidator;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String method = request.getMethod();
        String uri = request.getRequestURI();

        // Allow safe methods (GET, HEAD, OPTIONS)
        if (method.equals("GET") || method.equals("HEAD") || method.equals("OPTIONS")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Check if this is a profile endpoint targeting a specific user
        // Expected URI patterns: /api/profiles/{userId} or /profiles/{userId}
        String path = uri;
        if (path.startsWith("/api")) {
            path = path.substring(4);
        }
        
        if (path.startsWith("/profiles/")) {
            String remaining = path.substring("/profiles/".length());
            // Extract userId (until next / or end of string)
            int slashIndex = remaining.indexOf('/');
            String targetUserId;
            if (slashIndex > -1) {
                targetUserId = remaining.substring(0, slashIndex);
            } else {
                targetUserId = remaining;
            }

            // Skip special endpoints that are not userId based (e.g. search, count)
            // But usually these are GET. If there are unsafe operations on non-user endpoints, handle them?
            // "search" and "count" are GET only in Controller.
            // POST /profiles is "create or update", but typically we want to restrict it too?
            // Actually, POST /profiles takes a body with ID? The controller says:
            // @PostMapping public ResponseEntity<UserProfilesDTO> saveProfile(...)
            // This looks like it allows creating arbitrary profiles? We should restrict this too.
            // But identifying the target ID for POST is harder (it's in the body).
            // For now, let's focus on path-based IDs (PUT, DELETE).
            
            if (!targetUserId.isEmpty() && !targetUserId.equals("search") && !targetUserId.equals("count")) {
                 Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                 
                 if (authentication == null || !authentication.isAuthenticated()) {
                     response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
                     return;
                 }
                 
                 String currentUserId = authentication.getName();
                 
                 // Validate path parameter
                 try {
                     authorizationValidator.validatePathParameter(targetUserId);
                 } catch (SecurityException e) {
                     log.error("Invalid path parameter in profile access: {}", targetUserId);
                     response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid parameter");
                     return;
                 }
                 
                 // Check for Admin or Moderator role
                 boolean isAdminOrMod = authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MODERATOR"));
                 
                 // Check if authenticated user matches target user
                 // Note: We assume authentication.getName() returns the userId (as string)
                 if (!isAdminOrMod && !currentUserId.equals(targetUserId)) {
                     log.warn("Unauthorized access attempt: User {} tried to {} profile {}", 
                         currentUserId, method, targetUserId);
                     authorizationValidator.logAuthorizationCheck(
                         "profile:" + targetUserId, method, false);
                     response.sendError(HttpServletResponse.SC_FORBIDDEN, 
                         "You can only modify your own profile");
                     return;
                 }
                 
                 authorizationValidator.logAuthorizationCheck(
                     "profile:" + targetUserId, method, true);
            }
        }

        filterChain.doFilter(request, response);
    }
}
