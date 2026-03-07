package com.webapp.backend.controller;

import com.webapp.backend.dto.PostDTO;
import com.webapp.backend.service.PostService;
import com.webapp.backend.repository.UserProfileRepository;
import com.webapp.backend.repository.UserRepository;
import com.webapp.backend.model.UserProfile;
import com.webapp.backend.model.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Post REST Controller
 * 
 * Presentation Layer for Post API endpoints.
 * Handles HTTP requests and responses for social media posts.
 */
@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Posts", description = "Social media post management APIs")
@CrossOrigin(origins = "*")
public class PostController {

    private final PostService postService;
    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;

    @PostMapping
    @Operation(summary = "Create a new post")
    public ResponseEntity<?> createPost(@Valid @RequestBody PostDTO postDTO) {
        try {
            Long userId = null;
            // Extract user ID from security context
            try {
                var authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.isAuthenticated()) {
                    Object principal = authentication.getPrincipal();
                    if (principal instanceof String) {
                        String principalStr = (String) principal;
                        // Try to parse numeric ID first (local user)
                        if (principalStr.matches("\\d+")) {
                            userId = Long.parseLong(principalStr);
                        } else {
                            // Try to look up by username
                            Optional<UserProfile> profile = userProfileRepository.findByUsername(principalStr);
                            if (profile.isPresent()) {
                                userId = profile.get().getId();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Error extracting user ID from security context: {}", e.getMessage());
            }

            // Validate that we have a userId before proceeding
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Could not authenticate request or resolve user identity"));
            }

            PostDTO created = postService.createPost(postDTO, userId);
            return new ResponseEntity<>(created, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("Unexpected error creating post", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage(), "type", e.getClass().getSimpleName()));
        }
    }

    @GetMapping("/pending")
    @Operation(summary = "Get all pending posts (Admin/Moderator only)")
    public ResponseEntity<?> getPendingPosts() {
        if (!isAdminOrModerator()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Access denied. Requires ADMIN or MODERATOR role."));
        }
        
        log.info("REST request to get pending posts");
        List<PostDTO> posts = postService.getPendingPosts();
        return ResponseEntity.ok(posts);
    }

    @PutMapping("/{postId}/status")
    @Operation(summary = "Update post status (e.g. APPROVED, REJECTED)")
    public ResponseEntity<?> updatePostStatus(
            @PathVariable Long postId, 
            @RequestParam String status) {
        if (!isAdminOrModerator()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Access denied. Requires ADMIN or MODERATOR role."));
        }

        log.info("REST request to update post {} status to {}", postId, status);
        try {
             PostDTO updated = postService.updatePostStatus(postId, status);
             if (updated == null) {
                 return ResponseEntity.notFound().build();
             }
             return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{postId}/reject")
    @Operation(summary = "Reject and fully delete a post (Admin/Moderator only)")
    public ResponseEntity<?> rejectPost(@PathVariable Long postId) {
        if (!isAdminOrModerator()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Access denied. Requires ADMIN or MODERATOR role."));
        }

        log.info("REST request to reject and delete post: {}", postId);
        try {
            postService.deletePostWithCleanup(postId);
            return ResponseEntity.ok(Map.of("message", "Post rejected and deleted successfully", "postId", postId));
        } catch (Exception e) {
            log.error("Failed to reject post {}: {}", postId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to delete post: " + e.getMessage()));
        }
    }
    
    private boolean isAdminOrModerator() {
        try {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof String) {
                    String principalStr = (String) principal;
                    Long userId = null;
                    
                    if (principalStr.matches("\\d+")) {
                        userId = Long.parseLong(principalStr);
                    } else {
                        var profile = userProfileRepository.findByUsername(principalStr);
                        if (profile.isPresent()) userId = profile.get().getId();
                    }
                    
                    if (userId != null) {
                        var user = userRepository.findByUserProfile_Id(userId);
                        if (user.isPresent()) {
                            Role role = user.get().getRole();
                            return role == Role.ADMIN || role == Role.MODERATOR;
                        }
                    }
                }
            }
        } catch (Exception e) {
             log.warn("Security check failed", e);
        }
        return false;
    }

    @GetMapping("/{postId}")
    @Operation(summary = "Get post by ID")
    public ResponseEntity<?> getPostById(@PathVariable Long postId) {
        log.info("REST request to get post by ID: {}", postId);
        
        try {
            PostDTO post = postService.getPostById(postId);
            
            if (post == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Post not found"));
            }
            
            String currentUserId = null;
            try {
                var authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.isAuthenticated()) {
                    Object principal = authentication.getPrincipal();
                    if (principal instanceof String) {
                        currentUserId = (String) principal;
                    }
                }
            } catch (Exception e) {
                log.warn("Error extracting user ID from security context: {}", e.getMessage());
            }
            
            boolean isOwner = currentUserId != null && 
                             currentUserId.equals(String.valueOf(post.getUserProfileId()));
            boolean isPublic = Boolean.TRUE.equals(post.getIsPublished()) && 
                              !Boolean.TRUE.equals(post.getIsHidden()) && 
                              !Boolean.TRUE.equals(post.getIsDeleted()) &&
                              "APPROVED".equals(post.getStatus());
            
            if (!isPublic && !isOwner) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "This post is private or unavailable"));
            }
            
            return ResponseEntity.ok(post);
        } catch (Exception e) {
            log.error("Error retrieving post {}: {}", postId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to retrieve post", "details", e.getMessage()));
        }
    }

    @GetMapping
    @Operation(summary = "Get all posts")
    public ResponseEntity<List<PostDTO>> getAllPosts(
        @RequestParam(required = false) String gameCategory,
        @RequestParam(required = false) String videoType,
        @RequestParam(required = false) Long userId) {
        
        log.info("REST request to get posts with filters - gameCategory: {}, videoType: {}, userId: {}", 
                gameCategory, videoType, userId);
        
        List<PostDTO> posts;
        
        if (gameCategory != null || videoType != null || userId != null) {
            posts = postService.getPostsByFilters(gameCategory, videoType, userId);
        } else {
            posts = postService.getAllPosts();
        }
        
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get posts by user ID (accepts numeric id or user uid)")
    public ResponseEntity<List<PostDTO>> getPostsByUserId(@PathVariable String userId) {
        log.info("REST request to get posts by user identifier: {}", userId);
        Long numericId = null;
        // Try parse numeric id first
        if (userId != null && userId.matches("\\d+")) {
            try { numericId = Long.parseLong(userId); } catch (NumberFormatException ignored) {}
        }

        if (numericId == null) return ResponseEntity.ok(List.of());
        List<PostDTO> posts = postService.getPostsByUserId(numericId);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/game/{gameCategory}")
    @Operation(summary = "Get posts by game category")
    public ResponseEntity<List<PostDTO>> getPostsByGameCategory(@PathVariable String gameCategory) {
        log.info("REST request to get posts by game category: {}", gameCategory);
        List<PostDTO> posts = postService.getPostsByGameCategory(gameCategory);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/type/{videoType}")
    @Operation(summary = "Get posts by video type")
    public ResponseEntity<List<PostDTO>> getPostsByVideoType(@PathVariable String videoType) {
        log.info("REST request to get posts by video type: {}", videoType);
        List<PostDTO> posts = postService.getPostsByVideoType(videoType);
        return ResponseEntity.ok(posts);
    }

    @PutMapping("/{postId}")
    @Operation(summary = "Update post")
    public ResponseEntity<PostDTO> updatePost(@PathVariable Long postId, @Valid @RequestBody PostDTO postDTO) {
        log.info("REST request to update post: {}", postId);
        PostDTO updated = postService.updatePost(postId, postDTO);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{postId}")
    @Operation(summary = "Delete post")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId) {
        log.info("REST request to delete post: {}", postId);
        postService.deletePost(postId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{postId}/reactions")
    @Operation(summary = "Add reaction to post")
    public ResponseEntity<PostDTO> addReaction(
            @PathVariable Long postId,
            @RequestParam String userId,
            @RequestParam String reactionType) {
        log.info("REST request to add reaction {} to post {} by user {}", reactionType, postId, userId);
        PostDTO updated = postService.addReaction(postId, userId, reactionType);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{postId}/reactions")
    @Operation(summary = "Remove reaction from post")
    public ResponseEntity<PostDTO> removeReaction(
            @PathVariable Long postId,
            @RequestParam String userId,
            @RequestParam String reactionType) {
        log.info("REST request to remove reaction {} from post {} by user {}", reactionType, postId, userId);
        PostDTO updated = postService.removeReaction(postId, userId, reactionType);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/count")
    @Operation(summary = "Get total posts count")
    public ResponseEntity<Long> getTotalPostsCount() {
        log.info("REST request to get total posts count");
        long count = postService.getTotalPostsCount();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/count/user/{userId}")
    @Operation(summary = "Get posts count by user (accepts numeric id or user uid)")
    public ResponseEntity<Long> getPostsCountByUser(@PathVariable String userId) {
        log.info("REST request to get posts count by user identifier: {}", userId);
        Long numericId = null;
        if (userId != null && userId.matches("\\d+")) {
            try { numericId = Long.parseLong(userId); } catch (NumberFormatException ignored) {}
        }

        if (numericId == null) return ResponseEntity.ok(0L);
        long count = postService.getPostsCountByUser(numericId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/likes/user/{userId}")
    @Operation(summary = "Get total likes received across all posts by user")
    public ResponseEntity<Long> getTotalLikesReceivedByUser(@PathVariable String userId) {
        log.info("REST request to get total likes received by user: {}", userId);
        Long numericId = null;
        if (userId != null && userId.matches("\\d+")) {
            try { numericId = Long.parseLong(userId); } catch (NumberFormatException ignored) {}
        }

        if (numericId == null) return ResponseEntity.ok(0L);
        long total = postService.getTotalLikesReceivedByUser(numericId);
        return ResponseEntity.ok(total);
    }
}