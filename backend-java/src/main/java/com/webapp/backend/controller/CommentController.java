package com.webapp.backend.controller;

import com.webapp.backend.dto.CommentsDTO;
import com.webapp.backend.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Comment REST Controller
 * 
 * Presentation Layer for Comment API endpoints.
 * Handles HTTP requests and responses for post comments.
 */
@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Comments", description = "Post comment management APIs")
@CrossOrigin(origins = "*")
public class CommentController {

    private final CommentService commentService;
    private final com.webapp.backend.repository.UserProfileRepository userProfileRepository;

    @PostMapping
    @Operation(summary = "Create a new comment")
    public ResponseEntity<CommentsDTO> createComment(@Valid @RequestBody CommentsDTO commentDTO) {

        CommentsDTO created = commentService.createComment(commentDTO);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/{commentId}")
    @Operation(summary = "Get comment by ID")
    public ResponseEntity<CommentsDTO> getCommentById(@PathVariable Long commentId) {
        CommentsDTO comment = commentService.getCommentById(commentId);
        return ResponseEntity.ok(comment);
    }

    @GetMapping("/posts/{postId}")
    @Operation(summary = "Get comments by post ID")
    public ResponseEntity<List<CommentsDTO>> getCommentsByPostId(@PathVariable Long postId) {
       
        List<CommentsDTO> comments = commentService.getCommentsByPostId(postId);
        return ResponseEntity.ok(comments);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get comments by user ID (accepts numeric id)")
    public ResponseEntity<List<CommentsDTO>> getCommentsByUserId(@PathVariable String userId) {
        log.info("REST request to get comments by user identifier: {}", userId);
        Long numericId = null;
        // Try parse numeric id first
        if (userId != null && userId.matches("\\d+")) {
            try { numericId = Long.parseLong(userId); } catch (NumberFormatException ignored) {}
        } else {
            // Try to resolve from user_profiles by username
            try {
                var maybe = userProfileRepository.findByUsername(userId);
                if (maybe.isPresent()) numericId = maybe.get().getId();
            } catch (Exception e) {
                log.warn("Error resolving username to local id: {}", e.getMessage());
            }
        }

        if (numericId == null) return ResponseEntity.ok(List.of());
        List<CommentsDTO> comments = commentService.getCommentsByUserId(numericId);
        return ResponseEntity.ok(comments);
    }

    @PutMapping("/{commentId}")
    @Operation(summary = "Update comment")
    public ResponseEntity<CommentsDTO> updateComment(@PathVariable Long commentId, @Valid @RequestBody CommentsDTO commentDTO) {
     
        CommentsDTO updated = commentService.updateComment(commentId, commentDTO);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "Delete comment")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
      
        commentService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/posts/{postId}")
    @Operation(summary = "Delete all comments for a post")
    public ResponseEntity<Void> deleteCommentsByPostId(@PathVariable Long postId) {
        commentService.deleteCommentsByPostId(postId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/user/{userId}")
    @Operation(summary = "Delete all comments by a user (accepts numeric id)")
    public ResponseEntity<Void> deleteCommentsByUserId(@PathVariable String userId) {
        log.info("REST request to delete comments by user identifier: {}", userId);
        Long numericId = null;
        // Try parse numeric id first
        if (userId != null && userId.matches("\\d+")) {
            try { numericId = Long.parseLong(userId); } catch (NumberFormatException ignored) {}
        } else {
            // Try to resolve from user_profiles by username
            try {
                var maybe = userProfileRepository.findByUsername(userId);
                if (maybe.isPresent()) numericId = maybe.get().getId();
            } catch (Exception e) {
                log.warn("Error resolving username to local id: {}", e.getMessage());
            }
        }

        if (numericId == null) return ResponseEntity.noContent().build();
        commentService.deleteCommentsByUserId(numericId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count/posts/{postId}")
    @Operation(summary = "Get comments count for a post")
    public ResponseEntity<Long> getCommentsCountByPostId(@PathVariable Long postId) {
        long count = commentService.getCommentsCountByPostId(postId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/count/user/{userId}")
    @Operation(summary = "Get comments count by user (accepts numeric id)")
    public ResponseEntity<Long> getCommentsCountByUserId(@PathVariable String userId) {
        log.info("REST request to get comments count by user identifier: {}", userId);
        Long numericId = null;
        // Try parse numeric id first
        if (userId != null && userId.matches("\\d+")) {
            try { numericId = Long.parseLong(userId); } catch (NumberFormatException ignored) {}
        } else {
            // Try to resolve from user_profiles by username
            try {
                var maybe = userProfileRepository.findByUsername(userId);
                if (maybe.isPresent()) numericId = maybe.get().getId();
            } catch (Exception e) {
                log.warn("Error resolving username to local id: {}", e.getMessage());
            }
        }

        if (numericId == null) return ResponseEntity.ok(0L);
        long count = commentService.getCommentsCountByUserId(numericId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/count")
    @Operation(summary = "Get total comments count")
    public ResponseEntity<Long> getTotalCommentsCount() {
        long count = commentService.getTotalCommentsCount();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/{parentCommentId}/replies")
    @Operation(summary = "Get all replies to a comment")
    public ResponseEntity<List<CommentsDTO>> getRepliesByParentCommentId(@PathVariable Long parentCommentId) {
        List<CommentsDTO> replies = commentService.getRepliesByParentCommentId(parentCommentId);
        return ResponseEntity.ok(replies);
    }

    @GetMapping("/posts/{postId}/top-level")
    @Operation(summary = "Get top-level comments for a post (excluding replies)")
    public ResponseEntity<List<CommentsDTO>> getTopLevelCommentsByPostId(@PathVariable Long postId) {
        List<CommentsDTO> comments = commentService.getTopLevelCommentsByPostId(postId);
        return ResponseEntity.ok(comments);
    }

    @PostMapping("/{commentId}/like")
    @Operation(summary = "Like a comment")
    public ResponseEntity<CommentsDTO> likeComment(
            @PathVariable Long commentId,
            @RequestParam String userId) {
        CommentsDTO updated = commentService.likeComment(commentId, userId);
        return ResponseEntity.ok(updated);
    }
}