package com.webapp.backend.service.impl;

import com.webapp.backend.exception.ResourceNotFoundException;
import com.webapp.backend.exception.DuplicateResourceException;
import com.webapp.backend.mapper.GameMapper;
import com.webapp.backend.dto.GameDTO;
import com.webapp.backend.model.Game;
import com.webapp.backend.repository.GameRepository;
import com.webapp.backend.service.GameService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Game Service Implementation
 * 
 * Business Logic Layer implementation for Game operations.
 * Uses local database (SQLite/H2) for all game operations.
 */
@Service
@Transactional
@Slf4j
public class GameServiceImpl implements GameService {

    private final GameRepository gameRepository;
    private final GameMapper gameMapper;

    public GameServiceImpl(GameRepository gameRepository, GameMapper gameMapper) {
        this.gameRepository = gameRepository;
        this.gameMapper = gameMapper;
    }

    @Override
    public GameDTO createGame(GameDTO gameDTO) {
        
        // Check if game already exists
        if (gameRepository.existsByNameIgnoreCase(gameDTO.getName())) {
            throw new DuplicateResourceException("Game with name '" + gameDTO.getName() + "' already exists");
        }

        Game game = gameMapper.toEntity(gameDTO);
        Game savedGame = gameRepository.save(game);
        
        return gameMapper.toDTO(savedGame);
    }

    @Override
    @Transactional(readOnly = true)
    public GameDTO getGameById(Long id) {
        
        Game game = gameRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Game not found with ID: " + id));
        
        return gameMapper.toDTO(game);
    }

    

    @Override
    @Transactional(readOnly = true)
    public GameDTO getGameByName(String name) {
        
        Game game = gameRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> new ResourceNotFoundException("Game not found with name: " + name));
        
        return gameMapper.toDTO(game);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GameDTO> getAllGames() {
        
        List<Game> games = gameRepository.findAll();
        return gameMapper.toDTOList(games);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GameDTO> getActiveGames() {
        
        List<Game> games = gameRepository.findAllActiveGamesSortedByName();
        List<GameDTO> gameDTOs = gameMapper.toDTOList(games);
        
        return gameDTOs;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GameDTO> getInactiveGames() {
        
        List<Game> games = gameRepository.findByIsActiveFalse();
        return gameMapper.toDTOList(games);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GameDTO> searchGamesByName(String name) {
        
        List<Game> games = gameRepository.searchByName(name);
        return gameMapper.toDTOList(games);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GameDTO> getGamesByGenre(String genre) {
        
        List<Game> games = gameRepository.findByGenreIgnoreCase(genre);
        return gameMapper.toDTOList(games);
    }

    @Override
    public GameDTO updateGame(Long id, GameDTO gameDTO) {
        
        Game existingGame = gameRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Game not found with ID: " + id));

        // Check for duplicate name if name is being changed
        if (!existingGame.getName().equalsIgnoreCase(gameDTO.getName()) &&
            gameRepository.existsByNameIgnoreCase(gameDTO.getName())) {
            throw new DuplicateResourceException("Game with name '" + gameDTO.getName() + "' already exists");
        }

        gameMapper.updateGameFromDTO(gameDTO, existingGame);
        Game updatedGame = gameRepository.save(existingGame);
        
        return gameMapper.toDTO(updatedGame);
    }

    @Override
    public GameDTO activateGame(Long id) {
        
        Game game = gameRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Game not found with ID: " + id));
        
        game.setIsActive(true);
        Game updatedGame = gameRepository.save(game);
        
        return gameMapper.toDTO(updatedGame);
    }

    @Override
    public GameDTO deactivateGame(Long id) {
        
        Game game = gameRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Game not found with ID: " + id));
        
        game.setIsActive(false);
        Game updatedGame = gameRepository.save(game);
        
        return gameMapper.toDTO(updatedGame);
    }

    @Override
    public void deleteGame(Long id) {
        
        if (!gameRepository.existsById(id)) {
            throw new ResourceNotFoundException("Game not found with ID: " + id);
        }
        
        gameRepository.deleteById(id);
    }

    

    @Override
    @Transactional(readOnly = true)
    public long getTotalGameCount() {
        return gameRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long getActiveGameCount() {
        return gameRepository.countByIsActiveTrue();
    }

}
