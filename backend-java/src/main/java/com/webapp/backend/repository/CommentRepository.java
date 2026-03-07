package com.webapp.backend.repository;

import com.webapp.backend.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
	List<Comment> findByPostId(Long postId);
	
	List<Comment> findByUserId(Long userId);
	
	// Get replies to a specific comment
	List<Comment> findByParentCommentId(Long parentCommentId);
	
	// Get all top-level comments for a post (parent_comment_id IS NULL)
	@Query("SELECT c FROM Comment c WHERE c.post.id = :postId AND c.parentComment IS NULL ORDER BY c.createdAt DESC")
	List<Comment> findTopLevelCommentsByPostId(@Param("postId") Long postId);
	
	@Modifying(clearAutomatically = true)
	@Query("UPDATE Comment c SET c.ppUrl = :ppUrl WHERE c.userId = :userId")
	int updatePpUrlByUserId(@Param("userId") Long userId, @Param("ppUrl") String ppUrl);
	
	@Modifying(clearAutomatically = true)
	@Query("UPDATE Comment c SET c.userDisplayName = :displayName WHERE c.userId = :userId")
	int updateDisplayNameByUserId(@Param("userId") Long userId, @Param("displayName") String displayName);
}

