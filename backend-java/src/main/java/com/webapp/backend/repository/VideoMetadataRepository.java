package com.webapp.backend.repository;

import com.webapp.backend.model.VideoMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * VideoMetadata Repository
 * Handles database operations for video metadata
 */
@Repository
public interface VideoMetadataRepository extends JpaRepository<VideoMetadata, Long> {

    /**
     * Find video by post ID
     */
    Optional<VideoMetadata> findByPostIdAndIsDeletedFalse(Long postId);

    /**
     * Find all videos for a post (including deleted)
     */
    List<VideoMetadata> findByPostId(Long postId);

    /**
     * Find videos by storage type
     */
    @Query("SELECT v FROM VideoMetadata v WHERE v.storageType = :storageType AND v.isDeleted = false")
    List<VideoMetadata> findByStorageType(@Param("storageType") String storageType);

    /**
     * Find all non-deleted videos
     */
    @Query("SELECT v FROM VideoMetadata v WHERE v.isDeleted = false ORDER BY v.createdAt DESC")
    List<VideoMetadata> findAllActive();

    /**
     * Check if video exists for post
     */
    boolean existsByPostIdAndIsDeletedFalse(Long postId);

    /**
     * Find video by post relationship
     */
    @Query("SELECT v FROM VideoMetadata v WHERE v.post.id = :postId AND v.isDeleted = false")
    Optional<VideoMetadata> findByPostIdActive(@Param("postId") Long postId);
}
