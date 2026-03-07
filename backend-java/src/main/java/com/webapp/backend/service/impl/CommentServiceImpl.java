package com.webapp.backend.service.impl;

import com.webapp.backend.dto.CommentsDTO;
import com.webapp.backend.model.Comment;
import com.webapp.backend.model.Notification;
import com.webapp.backend.model.Post;
import com.webapp.backend.model.UserProfile;
import com.webapp.backend.repository.CommentRepository;
import com.webapp.backend.repository.PostRepository;
import com.webapp.backend.repository.UserProfileRepository;
import com.webapp.backend.mapper.CommentMapper;
import com.webapp.backend.service.CommentService;
import com.webapp.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;

/**
 * Comment Service Implementation
 * 
 * Business Logic Layer for comment operations.
 * Handles persistence of comments to PostgreSQL database.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final UserProfileRepository userProfileRepository;
    private final PostRepository postRepository;
    private final NotificationService notificationService;
    private final CommentMapper commentMapper;

    @Override
    public CommentsDTO createComment(CommentsDTO commentDTO) {
        log.info("Creating new comment for post: {} by user: {}", commentDTO.getPostId(), commentDTO.getUserId());
        
        // Validate required fields
        if (commentDTO.getPostId() == null) {
            log.error("Cannot create comment: postId is null");
            throw new IllegalArgumentException("postId cannot be null");
        }
        if (commentDTO.getContent() == null || commentDTO.getContent().isBlank()) {
            log.error("Cannot create comment: content is null or blank");
            throw new IllegalArgumentException("content cannot be null or blank");
        }
        
        try {
            // Convert DTO to entity
            Comment comment = commentMapper.toEntity(commentDTO);
            comment.setCreatedAt(LocalDateTime.now());
            comment.setUpdatedAt(LocalDateTime.now());
            
            // Set default values if null (Mapper might set them to null)
            if (comment.getLikesCount() == null) comment.setLikesCount(0);
            if (comment.getReportsCount() == null) comment.setReportsCount(0);
            if (comment.getIsDeleted() == null) comment.setIsDeleted(false);
            if (comment.getIsHidden() == null) comment.setIsHidden(false);
            
            // Fetch user profile to get display name and profile picture
            if (commentDTO.getUserProfileId() != null) {
                Optional<UserProfile> userProfile = userProfileRepository.findById(commentDTO.getUserProfileId());
                if (userProfile.isPresent()) {
                    comment.setUserProfile(userProfile.get());
                    log.debug("Set user profile for comment");
                } else {
                    log.warn("User profile not found for User ID: {}", commentDTO.getUserProfileId());
                }
            }
            
            // Fetch and set Post entity (Required)
            Post post = postRepository.findById(commentDTO.getPostId())
                    .orElseThrow(() -> new IllegalArgumentException("Post not found with ID: " + commentDTO.getPostId()));
            comment.setPost(post);
            
            // Fetch and set Parent Comment (Optional - for replies)
            if (commentDTO.getParentCommentId() != null) {
                Comment parent = commentRepository.findById(commentDTO.getParentCommentId())
                        .orElseThrow(() -> new IllegalArgumentException("Parent comment not found with ID: " + commentDTO.getParentCommentId()));
                comment.setParentComment(parent);
            }
            
            // Save to database
            Comment saved = commentRepository.save(comment);
            
            // Create notification for post owner (if available)
            try {
                // Post is already fetched above
                UserProfile postOwner = post.getUserProfile();
                String receiverUserId = null;
                if (postOwner != null) {
                    receiverUserId = String.valueOf(postOwner.getId());
                }
                
                if (receiverUserId != null && !receiverUserId.isBlank()
                        && !receiverUserId.equals(String.valueOf(commentDTO.getUserProfileId()))) {
                        
                        Notification notification = Notification.builder()
                                .userProfile(postOwner)
                                .userId(postOwner.getId()) // Set mandatory userId
                                .post(post)
                                .comment(saved)
                                .type("COMMENT_CREATED")
                                .title("New comment on your post")
                                .content(commentDTO.getContent())
                                .isRead(false)
                                .isDeleted(false)
                                .isHidden(false)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();
                        notificationService.createNotification(notification);
                        log.info("Notification created for post owner {} on new comment {}", receiverUserId, saved.getId());
                }
            } catch (Exception ex) {
                log.warn("Failed to create notification for new comment {}: {}", commentDTO.getPostId(), ex.getMessage());
            }
            
            log.info("Comment created successfully with ID: {}", saved.getId());
            return commentMapper.toDto(saved);
        } catch (Exception e) {
            log.error("Error creating comment: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create comment: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CommentsDTO getCommentById(Long commentId) {
        log.info("Fetching comment by ID: {}", commentId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> {
                    log.warn("Comment not found with ID: {}", commentId);
                    return new RuntimeException("Comment not found with ID: " + commentId);
                });

        log.info("Comment fetched successfully: {}", commentId);
        return commentMapper.toDto(comment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentsDTO> getCommentsByPostId(Long postId) {
        log.info("Fetching comments for post: {}", postId);

        List<Comment> postComments = commentRepository.findByPostId(postId);
        List<CommentsDTO> result = postComments.stream()
                .map(commentMapper::toDto)
                .collect(Collectors.toList());

        log.info("Found {} comments for post: {}", result.size(), postId);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentsDTO> getCommentsByUserId(Long userId) {
        log.info("Fetching comments by user (deprecated method - use userId): {}", userId);
        // This method is deprecated - userId should now be a String (userId)
        // Returning empty list as we migrated to User IDs
        return List.of();
    }

    @Override
    public CommentsDTO updateComment(Long commentId, CommentsDTO commentDTO) {
        log.info("Updating comment: {}", commentId);

        Comment existingComment = commentRepository.findById(commentId)
                .orElseThrow(() -> {
                    log.warn("Comment not found for update: {}", commentId);
                    return new RuntimeException("Comment not found with ID: " + commentId);
                });

        // Update fields
        existingComment.setContent(commentDTO.getContent());
        existingComment.setUpdatedAt(LocalDateTime.now());

        Comment updated = commentRepository.save(existingComment);

        log.info("Comment updated successfully: {}", commentId);
        return commentMapper.toDto(updated);
    }

    @Override
    public void deleteComment(Long commentId) {
        log.info("Deleting comment: {}", commentId);

        if (!commentRepository.existsById(commentId)) {
            log.warn("Comment not found for deletion: {}", commentId);
            throw new RuntimeException("Comment not found with ID: " + commentId);
        }

        commentRepository.deleteById(commentId);

        log.info("Comment deleted successfully: {}", commentId);
    }

    @Override
    public void deleteCommentsByPostId(Long postId) {
        log.info("Deleting all comments for post: {}", postId);

        List<Comment> postComments = commentRepository.findByPostId(postId);
        commentRepository.deleteAll(postComments);

        log.info("Deleted {} comments for post: {}", postComments.size(), postId);
    }

    @Override
    public void deleteCommentsByUserId(Long userId) {
        log.info("Deleting all comments by user (deprecated method - use userId): {}", userId);
        // This method is deprecated - userId should now be a String (userId)
        // No deletion as we migrated to User IDs
    }

    @Override
    @Transactional(readOnly = true)
    public long getCommentsCountByPostId(Long postId) {
        log.info("Getting comments count for post: {}", postId);

        long count = commentRepository.findByPostId(postId).size();

        log.info("Comments count for post {}: {}", postId, count);
        return count;
    }

    @Override
    @Transactional(readOnly = true)
    public long getCommentsCountByUserId(Long userId) {
        log.info("Getting comments count by user (deprecated method - use userId): {}", userId);
        // This method is deprecated - userId should now be a String (userId)
        return 0;
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalCommentsCount() {
        log.info("Getting total comments count");
        long count = commentRepository.count();
        log.info("Total comments count: {}", count);
        return count;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentsDTO> getRepliesByParentCommentId(Long parentCommentId) {
        log.info("Fetching replies for parent comment: {}", parentCommentId);
        
        // Verify parent comment exists
        commentRepository.findById(parentCommentId)
                .orElseThrow(() -> {
                    log.warn("Parent comment not found with ID: {}", parentCommentId);
                    return new RuntimeException("Parent comment not found with ID: " + parentCommentId);
                });
        
        List<Comment> replies = commentRepository.findByParentCommentId(parentCommentId);
        log.info("Found {} replies for parent comment: {}", replies.size(), parentCommentId);
        
        return replies.stream()
                .map(commentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentsDTO> getTopLevelCommentsByPostId(Long postId) {
        log.info("Fetching top-level comments for post: {}", postId);
        
        List<Comment> comments = commentRepository.findTopLevelCommentsByPostId(postId);
        log.info("Found {} top-level comments for post: {}", comments.size(), postId);
        
        return comments.stream()
                .map(commentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public CommentsDTO likeComment(Long commentId, String userId) {
        log.info("Liking comment {} by user {}", commentId, userId);
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found with ID: " + commentId));
        
        // Increment likes count
        Integer currentLikes = comment.getLikesCount() != null ? comment.getLikesCount() : 0;
        comment.setLikesCount(currentLikes + 1);
        comment.setUpdatedAt(LocalDateTime.now());
        
        // Create notification for comment owner (if not self-like)
        try {
            UserProfile commentOwner = comment.getUserProfile();
            String receiverUserId = null;
            if (commentOwner != null) {
                receiverUserId = String.valueOf(commentOwner.getId());
            }
            
            if (receiverUserId != null && !receiverUserId.isBlank()
                    && userId != null && !userId.isBlank()
                    && !receiverUserId.equals(userId)) {
                // in likeComment method
                Notification notification = Notification.builder()
                        .userProfile(commentOwner)
                        .userId(commentOwner.getId()) // Set mandatory userId
                        .post(comment.getPost())
                        .comment(comment)
                        .type("COMMENT_LIKED")
                        .title("Your comment received a new like")
                        .content("Someone liked your comment.")
                        .isRead(false)
                        .isDeleted(false)
                        .isHidden(false)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                notificationService.createNotification(notification);
                log.info("Like notification created for comment {} owner {}", commentId, receiverUserId);
            }
        } catch (Exception ex) {
            log.warn("Failed to create like notification for comment {}: {}", commentId, ex.getMessage());
        }
        
        Comment saved = commentRepository.save(comment);
        return commentMapper.toDto(saved);
    }
}
