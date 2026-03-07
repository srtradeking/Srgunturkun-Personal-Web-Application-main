package com.webapp.backend.controller;

import com.webapp.backend.mapper.PublicGameMapper;
import com.webapp.backend.dto.PublicGameDTO;
import com.webapp.backend.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public Game Controller
 * 
 * Provides public API endpoints for game data without authentication requirements.
 * Returns encapsulated DTOs with only public information, hiding internal metadata.
 */
@RestController
@RequestMapping("/public/games")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:8180", "http://localhost:5173", "http://localhost:3000"})
public class PublicGameController {

    private final GameRepository gameRepository;
    private final PublicGameMapper gameMapper;

    /**
     * Get all active games (public access)
     * Returns only basic game information without internal metadata
     */
    @GetMapping("/active")
    public ResponseEntity<List<PublicGameDTO>> getActiveGames() {
        try {
            
            var activeGames = gameRepository.findByIsActiveTrue();
            var publicGames = gameMapper.toPublicDTOList(activeGames);
            
            return ResponseEntity.ok(publicGames);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get game by ID (public access - active games only)
     */
    @GetMapping("/{gameId}")
    public ResponseEntity<PublicGameDTO> getGameById(@PathVariable Long gameId) {
        try {
            
            var gameOpt = gameRepository.findById(gameId);
            
            if (gameOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            var game = gameOpt.get();
            
            // Only return active games for public API
            if (!game.getIsActive()) {
                return ResponseEntity.notFound().build();
            }
            
            var publicGame = gameMapper.toPublicDTO(game);
            
            return ResponseEntity.ok(publicGame);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Search games by name (public access - active games only)
     */
    @GetMapping("/search")
    public ResponseEntity<List<PublicGameDTO>> searchGames(@RequestParam String name) {
        try {
            
            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            var allMatchingGames = gameRepository.searchByName(name.trim());
            // Filter to only active games for public API
            var activeMatchingGames = allMatchingGames.stream()
                    .filter(game -> game.getIsActive())
                    .toList();
                    
            var publicGames = gameMapper.toPublicDTOList(activeMatchingGames);
            
            return ResponseEntity.ok(publicGames);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get games by genre (public access - active games only)
     */
    @GetMapping("/genre/{genre}")
    public ResponseEntity<List<PublicGameDTO>> getGamesByGenre(@PathVariable String genre) {
        try {
            
            if (genre == null || genre.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            var allGenreGames = gameRepository.findByGenreIgnoreCase(genre.trim());
            // Filter to only active games for public API  
            var activeGenreGames = allGenreGames.stream()
                    .filter(game -> game.getIsActive())
                    .toList();
                    
            var publicGames = gameMapper.toPublicDTOList(activeGenreGames);
            
            return ResponseEntity.ok(publicGames);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}