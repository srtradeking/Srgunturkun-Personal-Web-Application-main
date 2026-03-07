package com.webapp.backend.service;

import com.webapp.backend.dto.GameDTO;

import java.util.List;

/**
 * Game Service Interface
 * 
 * Business Logic Layer interface for Game operations.
 * Defines all game-related business operations.
 */
public interface GameService {

    /**
     * Create a new game
     * @param gameDTO Game data
     * @return Created game
     */
    GameDTO createGame(GameDTO gameDTO);

    /**
     * Get game by ID
     * @param id Game ID
     * @return Game data
     */
    GameDTO getGameById(Long id);

    /**
     * Get game by name
     * @param name Game name
     * @return Game data
     */
    GameDTO getGameByName(String name);

    /**
     * Get all games
     * @return List of all games
     */
    List<GameDTO> getAllGames();

    /**
     * Get all active games
     * @return List of active games
     */
    List<GameDTO> getActiveGames();

    /**
     * Get all inactive games
     * @return List of inactive games
     */
    List<GameDTO> getInactiveGames();

    /**
     * Search games by name
     * @param name Search query
     * @return List of matching games
     */
    List<GameDTO> searchGamesByName(String name);

    /**
     * Get games by genre
     * @param genre Genre name
     * @return List of games in that genre
     */
    List<GameDTO> getGamesByGenre(String genre);

    /**
     * Update game
     * @param id Game ID
     * @param gameDTO Updated game data
     * @return Updated game
     */
    GameDTO updateGame(Long id, GameDTO gameDTO);

    /**
     * Activate game
     * @param id Game ID
     * @return Updated game
     */
    GameDTO activateGame(Long id);

    /**
     * Deactivate game
     * @param id Game ID
     * @return Updated game
     */
    GameDTO deactivateGame(Long id);

    /**
     * Delete game
     * @param id Game ID
     */
    void deleteGame(Long id);

    /**
     * Get total game count
     * @return Total number of games
     */
    long getTotalGameCount();

    /**
     * Get active game count
     * @return Number of active games
     */
    long getActiveGameCount();
}
