package com.webapp.backend.service;

import java.util.List;

import com.webapp.backend.dto.PostDTO;

/**
 * Post Service Interface
 * 
 * Business Logic Layer interface for Post operations.
 * Handles post-related business logic and data access.
 */
public interface PostService {
    
    /**
     * Create a new post
     */
    PostDTO createPost(PostDTO postDTO, Long userId);

    /**
     * Get post by database ID
     */
    PostDTO getPostById(Long postId);
    
    /**
     * Get all posts ordered by timestamp (newest first)
     */
    List<PostDTO> getAllPosts();
    
    /**
     * Get all pending posts for moderation
     */
    List<PostDTO> getPendingPosts();

    /**
     * Get posts by user ID
     */
    List<PostDTO> getPostsByUserId(Long userId);
    
    /**
     * Get posts by game category
     */
    List<PostDTO> getPostsByGameCategory(String gameCategory);
    
    /**
     * Get posts by video type (short/long)
     */
    List<PostDTO> getPostsByVideoType(String videoType);
    
    /**
     * Get posts by multiple filters
     */
    List<PostDTO> getPostsByFilters(String gameCategory, String videoType, Long userId);
    
    /**
     * Update post
     */
    PostDTO updatePost(Long postId, PostDTO postDTO);
    
    /**
     * Update post status (e.g. approve/reject)
     */
    PostDTO updatePostStatus(Long postId, String status);

    /**
     * Delete post
     */
    void deletePost(Long postId);

    /**
     * Delete post with full cleanup (R2 storage, comments, likes, notifications)
     * Used for moderation rejection
     */
    void deletePostWithCleanup(Long postId);
    /**
     * Delete all posts by a given user id (database numeric id)
     */
    void deletePostsByUserId(Long userId);
    
    /**
     * Add reaction to post
     */
    PostDTO addReaction(Long postId, String userIdentifier, String reactionType);
    
    /**
     * Remove reaction from post
     */
    PostDTO removeReaction(Long postId, String userIdentifier, String reactionType);
    
    /**
     * Get total posts count
     */
    long getTotalPostsCount();
    
    /**
     * Get posts count by user
     */
    long getPostsCountByUser(Long userId);
    
    /**
     * Get total likes received across all posts by user
     */
    long getTotalLikesReceivedByUser(Long userId);
}