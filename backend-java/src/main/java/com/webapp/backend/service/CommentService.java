package com.webapp.backend.service;

import java.util.List;

import com.webapp.backend.dto.CommentsDTO;

/**
 * Comment Service Interface
 * 
 * Business Logic Layer interface for Comment operations.
 * Handles comment-related business logic and data access.
 */
public interface CommentService {
    
    /**
     * Create a new comment
     */
    CommentsDTO createComment(CommentsDTO commentDTO);
    
    /**
     * Get comment by database ID
     */
    CommentsDTO getCommentById(Long commentId);
    
    /**
     * Get all comments for a specific post
     */
    List<CommentsDTO> getCommentsByPostId(Long postId);
    
    /**
     * Get all comments by a specific user
     */
    List<CommentsDTO> getCommentsByUserId(Long userId);
    
    /**
     * Update comment
     */
    CommentsDTO updateComment(Long commentId, CommentsDTO commentDTO);
    
    /**
     * Delete comment
     */
    void deleteComment(Long commentId);
    
    /**
     * Delete all comments for a post
     */
    void deleteCommentsByPostId(Long postId);
    
    /**
     * Delete all comments by a user
     */
    void deleteCommentsByUserId(Long userId);
    
    /**
     * Get comments count for a post
     */
    long getCommentsCountByPostId(Long postId);
    
    /**
     * Get total comments count by user
     */
    long getCommentsCountByUserId(Long userId);
    
    /**
     * Get total comments count
     */
    long getTotalCommentsCount();
    
    /**
     * Get all replies to a specific comment
     */
    List<CommentsDTO> getRepliesByParentCommentId(Long parentCommentId);
    
    /**
     * Get only top-level comments for a post (excluding replies)
     */
    List<CommentsDTO> getTopLevelCommentsByPostId(Long postId);
    
    /**
     * Like a comment (increments likesCount and may trigger notifications)
     */
    CommentsDTO likeComment(Long commentId, String userId);
}