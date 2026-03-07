package com.webapp.backend.repository;

import com.webapp.backend.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Game Repository
 * 
 * Data Access Layer for Game entity.
 * Provides CRUD operations and custom queries.
 */
@Repository
public interface GameRepository extends JpaRepository<Game, Long> {

    /**
     * Find game by name (case-insensitive)
     */
    Optional<Game> findByNameIgnoreCase(String name);

    

    /**
     * Find all active games
     */
    List<Game> findByIsActiveTrue();

    /**
     * Find all inactive games
     */
    List<Game> findByIsActiveFalse();

    /**
     * Find games by genre
     */
    List<Game> findByGenreIgnoreCase(String genre);

    /**
     * Search games by name (partial match, case-insensitive)
     */
    @Query("SELECT g FROM Game g WHERE LOWER(g.name) LIKE LOWER(CONCAT('%', :name, '%')) AND g.isActive = true")
    List<Game> searchByName(@Param("name") String name);

    /**
     * Find active games ordered by name
     */
    @Query("SELECT g FROM Game g WHERE g.isActive = true ORDER BY g.name ASC")
    List<Game> findAllActiveGamesSortedByName();

    /**
     * Find games by genre (active only)
     */
    @Query("SELECT g FROM Game g WHERE LOWER(g.genre) = LOWER(:genre) AND g.isActive = true")
    List<Game> findActiveGamesByGenre(@Param("genre") String genre);

    /**
     * Find games by platform (active only)
     */
    @Query("SELECT g FROM Game g WHERE LOWER(g.platform) = LOWER(:platform) AND g.isActive = true")
    List<Game> findActiveGamesByPlatform(@Param("platform") String platform);

    /**
     * Count active games
     */
    long countByIsActiveTrue();

    /**
     * Count games by genre
     */
    @Query("SELECT COUNT(g) FROM Game g WHERE LOWER(g.genre) = LOWER(:genre) AND g.isActive = true")
    long countActiveGamesByGenre(@Param("genre") String genre);

    /**
     * Count games by platform
     */
    @Query("SELECT COUNT(g) FROM Game g WHERE LOWER(g.platform) = LOWER(:platform) AND g.isActive = true")
    long countActiveGamesByPlatform(@Param("platform") String platform);

    /**
     * Check if game exists by name (case-insensitive)
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Check if active game exists by name (case-insensitive)
     */
    @Query("SELECT CASE WHEN COUNT(g) > 0 THEN true ELSE false END FROM Game g WHERE LOWER(g.name) = LOWER(:name) AND g.isActive = true")
    boolean existsActiveGameByName(@Param("name") String name);

    /**
     * Get distinct genres from active games
     */
    @Query("SELECT DISTINCT g.genre FROM Game g WHERE g.isActive = true AND g.genre IS NOT NULL ORDER BY g.genre")
    List<String> findDistinctActiveGenres();

    /**
     * Get distinct platforms from active games
     */
    @Query("SELECT DISTINCT g.platform FROM Game g WHERE g.isActive = true AND g.platform IS NOT NULL ORDER BY g.platform")
    List<String> findDistinctActivePlatforms();
}
