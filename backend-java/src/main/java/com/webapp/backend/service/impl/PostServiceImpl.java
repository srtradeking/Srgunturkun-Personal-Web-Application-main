package com.webapp.backend.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.webapp.backend.service.PostService;
import com.webapp.backend.dto.PostDTO;
import com.webapp.backend.model.Notification;
import com.webapp.backend.model.Post;
import com.webapp.backend.model.UserProfile;
import com.webapp.backend.repository.NotificationRepository;
import com.webapp.backend.repository.PostRepository;
import com.webapp.backend.repository.UserProfileRepository;
import com.webapp.backend.repository.PostLikeRepository;
import com.webapp.backend.model.PostLike;
import com.webapp.backend.mapper.PostMapper;

import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Post Service Implementation
 * 
 * Manages business logic for Post operations.
 * Integrates with database repository for persistence.
 */
@Slf4j
@Service
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final PostMapper postMapper;
    private final com.webapp.backend.service.CommentService commentService;
    private final UserProfileRepository userProfileRepository;
    private final NotificationRepository notificationRepository;
    private final com.webapp.backend.repository.VideoMetadataRepository videoMetadataRepository;
    private final PostLikeRepository postLikeRepository;
    private final com.webapp.backend.repository.UserRepository userRepository;

    public PostServiceImpl(PostRepository postRepository, PostMapper postMapper, 
                         com.webapp.backend.service.CommentService commentService,
                         UserProfileRepository userProfileRepository,
                         NotificationRepository notificationRepository,
                         com.webapp.backend.repository.VideoMetadataRepository videoMetadataRepository,
                         PostLikeRepository postLikeRepository,
                         com.webapp.backend.repository.UserRepository userRepository) {
        this.postRepository = postRepository;
        this.postMapper = postMapper;
        this.commentService = commentService;
        this.userProfileRepository = userProfileRepository;
        this.notificationRepository = notificationRepository;
        this.videoMetadataRepository = videoMetadataRepository;
        this.postLikeRepository = postLikeRepository;
        this.userRepository = userRepository;
        log.info("✅ PostService initialized with database-backed repository");
    }

    @Override
    public PostDTO createPost(PostDTO postDTO, Long userId) {
        log.info("Creating post for user: {}", userId);
        
        // Convert PostDTO to entity (this will properly map all fields)
        Post post = postMapper.toEntity(postDTO);
        
        // Determine initial status based on User Role
        // Default to PENDING for safety
        com.webapp.backend.model.PostStatus initialStatus = com.webapp.backend.model.PostStatus.PENDING;
        
        if (userId != null) {
            var user = userRepository.findByUserProfile_Id(userId);
            if (user.isPresent()) {
                var role = user.get().getRole();
                log.info("User {} has role: {}", userId, role);
                if (role == com.webapp.backend.model.Role.ADMIN || role == com.webapp.backend.model.Role.MODERATOR) {
                    initialStatus = com.webapp.backend.model.PostStatus.APPROVED;
                    log.info("Auto-approving post for privileged user");
                }
            } else {
                log.warn("User not found for ID: {}, defaulting to PENDING", userId);
            }
        } else {
            log.warn("No userId provided for post creation, defaulting to PENDING");
        }
        
        post.setStatus(initialStatus);
        log.info("Set initial post status to: {}", initialStatus);
        
        // Set default values for nullable fields that have constraints
        if (post.getLikesCount() == null) {
            post.setLikesCount(0);
        }
        if (post.getCommentsCount() == null) {
            post.setCommentsCount(0);
        }
        if (post.getReportsCount() == null) {
            post.setReportsCount(0);
        }
        if (post.getIsPublished() == null) {
            post.setIsPublished(true);
        }
        if (post.getIsDeleted() == null) {
            post.setIsDeleted(false);
        }
        if (post.getIsHidden() == null) {
            post.setIsHidden(false);
        }
        
        
        // Set userProfile if userId is provided
        if (userId != null) {
            try {
                var userProfile = userProfileRepository.findById(userId);
                if (userProfile.isPresent()) {
                    post.setUserProfile(userProfile.get());
                    log.debug("✅ Associated post with user profile: {}", userId);
                }
            } catch (Exception e) {
                log.warn("Could not associate user profile {}: {}", userId, e.getMessage());
            }
        }
        
        LocalDateTime now = LocalDateTime.now();
        post.setCreatedAt(now);
        post.setUpdatedAt(now);
        Post saved = postRepository.save(post);
        log.debug("✅ Created post: {}", saved.getId());
        return postMapper.toDto(saved);
    }

    @Override
    public PostDTO getPostById(Long postId) {
        return postRepository.findById(postId).map(postMapper::toDto).orElse(null);
    }

    @Override
    public List<PostDTO> getAllPosts() {
        return postRepository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsPublished()) && 
                            !Boolean.TRUE.equals(p.getIsHidden()) && 
                            !Boolean.TRUE.equals(p.getIsDeleted()) &&
                            p.getStatus() == com.webapp.backend.model.PostStatus.APPROVED)
                .sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()))
                .map(this::enrichPostDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PostDTO> getPendingPosts() {
        return postRepository.findByStatus(com.webapp.backend.model.PostStatus.PENDING).stream()
                .sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()))
                .map(this::enrichPostDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PostDTO> getPostsByUserId(Long userId) {
        if (userId == null) return List.of();
        return postRepository.findByUserId(userId).stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsPublished()) && 
                            !Boolean.TRUE.equals(p.getIsHidden()) && 
                            !Boolean.TRUE.equals(p.getIsDeleted()) &&
                            p.getStatus() == com.webapp.backend.model.PostStatus.APPROVED)
                .sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()))
                .map(this::enrichPostDTO)
                .collect(Collectors.toList());
    }

    private PostDTO enrichPostDTO(Post post) {
        PostDTO dto = postMapper.toDto(post);
        
        // Add description as alias for content (for frontend compatibility)
        dto.setDescription(post.getContent());
        
        // Get user profile for additional info
        UserProfile profile = post.getUserProfile();
        if (profile != null) {
            dto.setUserProfileId(profile.getId());
            dto.setUserDisplayName(profile.getDisplayName());
            dto.setPpUrl(profile.getProfilePictureUrl());
        }
        
        // Ensure default values
        if (dto.getLikesCount() == null) dto.setLikesCount(0);
        if (dto.getCommentsCount() == null) dto.setCommentsCount(0);
        if (dto.getReportsCount() == null) dto.setReportsCount(0);
        if (dto.getIsPublished() == null) dto.setIsPublished(true);
        if (dto.getIsDeleted() == null) dto.setIsDeleted(false);
        if (dto.getIsHidden() == null) dto.setIsHidden(false);
        
        // Map type and url for frontend compatibility
        if (dto.getImageUrl() != null && !dto.getImageUrl().isEmpty()) {
            dto.setType("image");
            dto.setUrl(dto.getImageUrl());
            if (!dto.getMimeType().contains("image")) {
                dto.setMimeType("image/jpeg");
            }
        } else if (dto.getVideoUrl() != null && !dto.getVideoUrl().isEmpty()) {
            dto.setType("video");
            dto.setUrl(dto.getVideoUrl());
            if (dto.getVideoUrl().endsWith(".mp4") || !dto.getMimeType().contains("video")) {
                dto.setMimeType("video/mp4");
            }
        }
        
        return dto;
    }

    @Override
    public List<PostDTO> getPostsByGameCategory(String gameCategory) {
        // Game category filtering removed - field no longer exists in new schema
        log.warn("getPostsByGameCategory() deprecated - gameCategory field removed in V29 schema");
        return getAllPosts();
    }

    @Override
    public List<PostDTO> getPostsByVideoType(String videoType) {
        // Video type filtering removed - field no longer exists in new schema
        log.warn("getPostsByVideoType() deprecated - videoType field removed in V29 schema");
        return getAllPosts();
    }

    @Override
    public List<PostDTO> getPostsByFilters(String gameCategory, String videoType, Long userId) {
        // Simplified to only filter by userId since gameCategory and videoType no longer exist
        if (userId != null) {
            return getPostsByUserId(userId);
        }
        return getAllPosts();
    }

    @Override
    public PostDTO updatePost(Long postId, PostDTO postDTO) {
        return postRepository.findById(postId).map(existing -> {
            if (postDTO.getTitle() != null) existing.setTitle(postDTO.getTitle());
            if (postDTO.getContent() != null) existing.setContent(postDTO.getContent());
            if (postDTO.getImageUrl() != null) existing.setImageUrl(postDTO.getImageUrl());
            
            // Handle Video URL update and Metadata synchronization
            if (postDTO.getVideoUrl() != null) {
                existing.setVideoUrl(postDTO.getVideoUrl());
                try {
                    // Update or Create VideoMetadata
                    var existingMetadata = videoMetadataRepository.findByPostIdActive(postId);
                    if (existingMetadata.isPresent()) {
                        var meta = existingMetadata.get();
                        meta.setVideoUrl(postDTO.getVideoUrl());
                        meta.setUpdatedAt(LocalDateTime.now());
                        videoMetadataRepository.save(meta);
                    } else {
                        var meta = com.webapp.backend.model.VideoMetadata.builder()
                            .post(existing)
                            .videoUrl(postDTO.getVideoUrl())
                            .storageType("local")
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .isDeleted(false)
                            .build();
                        videoMetadataRepository.save(meta);
                    }
                    log.debug("✅ Synced VideoMetadata for post: {}", postId);
                } catch (Exception e) {
                    log.error("Failed to update video metadata for post {}: {}", postId, e.getMessage());
                }
            }

            if (postDTO.getIsPublished() != null) existing.setIsPublished(postDTO.getIsPublished());
            if (postDTO.getIsDeleted() != null) existing.setIsDeleted(postDTO.getIsDeleted());
            if (postDTO.getIsHidden() != null) existing.setIsHidden(postDTO.getIsHidden());
            existing.setUpdatedAt(LocalDateTime.now());
            Post saved = postRepository.save(existing);
            log.debug("Updated post: {}", postId);
            return postMapper.toDto(saved);
        }).orElse(null);
    }

    @Override
    public PostDTO updatePostStatus(Long postId, String status) {
        return postRepository.findById(postId).map(existing -> {
            try {
                com.webapp.backend.model.PostStatus newStatus = com.webapp.backend.model.PostStatus.valueOf(status.toUpperCase());
                existing.setStatus(newStatus);
                existing.setUpdatedAt(LocalDateTime.now());
                Post saved = postRepository.save(existing);
                log.info("Updated post {} status to {}", postId, newStatus);
                return postMapper.toDto(saved);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid post status provided: {}", status);
                throw new IllegalArgumentException("Invalid post status: " + status);
            }
        }).orElse(null);
    }

    @Override
    public void deletePost(Long postId) {
        executePostDeletion(postId);
    }

    @Override
    @Transactional
    public void deletePostWithCleanup(Long postId) {
        executePostDeletion(postId);
    }
    
    private void executePostDeletion(Long postId) {
        log.info("🗑️ Full deletion requested for post: {}", postId);
        
        var maybePost = postRepository.findById(postId);
        if (maybePost.isEmpty()) {
            log.warn("Cannot delete non-existent post: {}", postId);
            return;
        }
        
        Post post = maybePost.get();
        
        // 1. Note: Media files are not automatically deleted from local storage
        // This can be implemented later with a cleanup job if needed
        
        // 2. Delete video metadata
        try {
            var metadata = videoMetadataRepository.findByPostIdActive(postId);
            if (metadata.isPresent()) {
                videoMetadataRepository.delete(metadata.get());
                log.debug("Deleted video metadata for post: {}", postId);
            }
        } catch (Exception e) {
            log.warn("Failed to delete video metadata for post {}: {}", postId, e.getMessage());
        }
        
        // 3. Delete comments
        try {
            commentService.deleteCommentsByPostId(postId);
            log.debug("Deleted comments for post: {}", postId);
        } catch (Exception e) {
            log.warn("Failed to delete comments for post {}: {}", postId, e.getMessage());
        }
        
        // 4. Delete likes
        try {
            postLikeRepository.deleteByPostId(postId);
            log.debug("Deleted likes for post: {}", postId);
        } catch (Exception e) {
            log.warn("Failed to delete likes for post {}: {}", postId, e.getMessage());
        }
        
        // 5. Delete notifications related to this post
        try {
            notificationRepository.deleteByPostId(postId);
            log.debug("Deleted notifications for post: {}", postId);
        } catch (Exception e) {
            log.warn("Failed to delete notifications for post {}: {}", postId, e.getMessage());
        }
        
        // 6. Delete the post itself
        try {
            postRepository.deleteById(postId);
            log.info("✅ Successfully deleted post with full cleanup: {}", postId);
        } catch (Exception e) {
            log.error("❌ Failed to delete post {}: {}", postId, e.getMessage());
            throw new RuntimeException("Failed to delete post", e);
        }
    }
    
    private String extractKeyFromUrl(String url) {
        if (url == null || url.isBlank()) return null;
        
        // Handle different URL formats:
        // 1. /api/storage/{key}
        // 2. https://worker.example.com/get/{key}?...
        // 3. Direct R2 URL with key at the end
        
        try {
            // Remove query parameters
            String cleanUrl = url.contains("?") ? url.substring(0, url.indexOf("?")) : url;
            
            // Get the last path segment as the key
            if (cleanUrl.contains("/")) {
                return cleanUrl.substring(cleanUrl.lastIndexOf("/") + 1);
            }
            return cleanUrl;
        } catch (Exception e) {
            log.warn("Failed to extract key from URL: {}", url);
            return null;
        }
    }

    @Override
    public void deletePostsByUserId(Long userId) {
        if (userId == null) return;
        var posts = postRepository.findByUserId(userId);
        for (var p : posts) {
            try {
                // delete comments related to the post first
                commentService.deleteCommentsByPostId(p.getId());
            } catch (Exception e) {
                log.warn("Failed to delete comments for post {}: {}", p.getId(), e.getMessage());
            }
            try {
                postRepository.deleteById(p.getId());
                log.debug("Deleted post: {}", p.getId());
            } catch (Exception e) {
                log.warn("Failed to delete post {}: {}", p.getId(), e.getMessage());
            }
        }
    }

    @Override
    public PostDTO addReaction(Long postId, String userIdentifier, String reactionType) {
        try {
            Long userId = Long.parseLong(userIdentifier);
            
            // Check if already liked
            if (postLikeRepository.existsByPostIdAndUserId(postId, userId)) {
                return getPostById(postId);
            }

            var maybePost = postRepository.findById(postId);
            if (maybePost.isPresent()) {
                Post post = maybePost.get();
                
                // Create and save like
                PostLike like = PostLike.builder()
                        .postId(postId)
                        .userId(userId)
                        .build();
                postLikeRepository.save(like);
                
                // Increment likes count
                post.setLikesCount(post.getLikesCount() + 1);
                postRepository.save(post);
                
                // Update user's coin balance (total likes point)
                UserProfile postOwner = post.getUserProfile();
                if (postOwner != null) {
                    long currentPoints = postOwner.getTotalLikesPoint() == null ? 0L : postOwner.getTotalLikesPoint();
                    postOwner.setTotalLikesPoint(currentPoints + 1);
                    userProfileRepository.save(postOwner);
                }
                
                String receiverUserId = null;
                if (postOwner != null) {
                    receiverUserId = String.valueOf(postOwner.getId());
                }
                
                // Do not notify if we cannot resolve the owner or if user likes own post
                if (receiverUserId != null && !receiverUserId.isBlank()
                        && userIdentifier != null && !userIdentifier.isBlank()
                        && !receiverUserId.equals(userIdentifier)) {
                    Notification notification = Notification.builder()
                            .userId(postOwner.getId()) // Fix: Set userId explicitly
                            .userProfile(postOwner)
                            .post(post)
                            .type("POST_LIKED")
                            .title("Your post received a new like")
                            .content("Someone liked your post.")
                            .isRead(false)
                            .isDeleted(false)
                            .isHidden(false)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    notificationRepository.save(notification);
                    log.info("Like notification created for post {} owner {}", postId, receiverUserId);
                }
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid user ID format for reaction: {}", userIdentifier);
        } catch (Exception ex) {
            log.warn("Failed to process reaction for post {}: {}", postId, ex.getMessage());
        }
        return getPostById(postId);
    }

    @Override
    public PostDTO removeReaction(Long postId, String userIdentifier, String reactionType) {
        try {
            Long userId = Long.parseLong(userIdentifier);
            
            var existingLike = postLikeRepository.findByPostIdAndUserId(postId, userId);
            if (existingLike.isPresent()) {
                postLikeRepository.delete(existingLike.get());
                
                var maybePost = postRepository.findById(postId);
                if (maybePost.isPresent()) {
                    Post post = maybePost.get();
                    // Decrement likes count but don't go below 0
                    if (post.getLikesCount() > 0) {
                        post.setLikesCount(post.getLikesCount() - 1);
                        postRepository.save(post);
                        
                        // Update user's coin balance (total likes point)
                        UserProfile postOwner = post.getUserProfile();
                        if (postOwner != null) {
                             long currentPoints = postOwner.getTotalLikesPoint() == null ? 0L : postOwner.getTotalLikesPoint();
                             if (currentPoints > 0) {
                                 postOwner.setTotalLikesPoint(currentPoints - 1);
                                 userProfileRepository.save(postOwner);
                             }
                        }
                    }
                }
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid user ID format for remove reaction: {}", userIdentifier);
        } catch (Exception ex) {
            log.warn("Failed to process remove reaction for post {}: {}", postId, ex.getMessage());
        }
        return getPostById(postId);
    }

    @Override
    public long getTotalPostsCount() {
        return postRepository.count();
    }

    @Override
    public long getPostsCountByUser(Long userId) {
        if (userId == null) return 0;
        return postRepository.findByUserId(userId).size();
    }

    @Override
    public long getTotalLikesReceivedByUser(Long userId) {
        if (userId == null) return 0;
        
        UserProfile profile = userProfileRepository.findById(userId).orElse(null);
        if (profile == null) return 0;
        
        // Lazy sync: if stored value is 0, check if real sum from posts is different
        // This handles migration for existing users who already have likes
        if (profile.getTotalLikesPoint() == 0) {
            Long realSum = postRepository.sumLikesCountByUserId(userId);
            if (realSum != null && realSum > 0) {
                log.info("Syncing totalLikesPoint for user {} from 0 to {}", userId, realSum);
                profile.setTotalLikesPoint(realSum);
                userProfileRepository.save(profile);
                return realSum;
            }
        }
        
        return profile.getTotalLikesPoint();
    }
}
