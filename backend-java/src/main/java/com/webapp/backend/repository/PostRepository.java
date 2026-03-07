package com.webapp.backend.repository;

import com.webapp.backend.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
	// TODO: Post entity doesn't have gameCategory or videoType properties
	// Use explicit @Query methods if filtering by video type is needed
	// List<Post> findByGameCategoryIgnoreCase(String gameCategory);
	// List<Post> findByVideoTypeIgnoreCase(String videoType);
	
	@Query("SELECT p FROM Post p WHERE p.userProfile.id = :userId")
	List<Post> findByUserId(Long userId);
	
	@Modifying(clearAutomatically = true)
	@Query("UPDATE Post p SET p.imageUrl = :imageUrl WHERE p.id = :postId")
	int updateImageUrlByPostId(@Param("postId") Long postId, @Param("imageUrl") String imageUrl);
	
	@Query("SELECT COALESCE(SUM(p.likesCount), 0) FROM Post p WHERE p.userProfile.id = :userId")
	Long sumLikesCountByUserId(@Param("userId") Long userId);

	List<Post> findByStatus(com.webapp.backend.model.PostStatus status);
}

