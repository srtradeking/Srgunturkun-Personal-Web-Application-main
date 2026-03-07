package com.webapp.backend.controller;

import com.webapp.backend.dto.GameDTO;
import com.webapp.backend.service.GameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Game REST Controller
 * 
 * Presentation Layer for Game API endpoints.
 * Handles HTTP requests and responses.
 */
@RestController
@RequestMapping("/games")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Games", description = "Game management APIs")
@CrossOrigin(origins = "*")
public class GameController {

    private final GameService gameService;

    @PostMapping
    @Operation(summary = "Create a new game")
    public ResponseEntity<GameDTO> createGame(@Valid @RequestBody GameDTO gameDTO) {
        GameDTO created = gameService.createGame(gameDTO);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get game by ID")
    public ResponseEntity<GameDTO> getGameById(@PathVariable Long id) {
        GameDTO game = gameService.getGameById(id);
        return ResponseEntity.ok(game);
    }


    @GetMapping("/name/{name}")
    @Operation(summary = "Get game by name")
    public ResponseEntity<GameDTO> getGameByName(@PathVariable String name) {
        GameDTO game = gameService.getGameByName(name);
        return ResponseEntity.ok(game);
    }

    @GetMapping
    @Operation(summary = "Get all games")
    public ResponseEntity<List<GameDTO>> getAllGames() {
        List<GameDTO> games = gameService.getAllGames();
        return ResponseEntity.ok(games);
    }

    @GetMapping("/active")
    @Operation(summary = "Get all active games")
    public ResponseEntity<List<GameDTO>> getActiveGames() {
        List<GameDTO> games = gameService.getActiveGames();
        return ResponseEntity.ok(games);
    }

    @GetMapping("/inactive")
    @Operation(summary = "Get all inactive games")
    public ResponseEntity<List<GameDTO>> getInactiveGames() {
        List<GameDTO> games = gameService.getInactiveGames();
        return ResponseEntity.ok(games);
    }

    @GetMapping("/search")
    @Operation(summary = "Search games by name")
    public ResponseEntity<List<GameDTO>> searchGames(@RequestParam String name) {
        List<GameDTO> games = gameService.searchGamesByName(name);
        return ResponseEntity.ok(games);
    }

    @GetMapping("/genre/{genre}")
    @Operation(summary = "Get games by genre")
    public ResponseEntity<List<GameDTO>> getGamesByGenre(@PathVariable String genre) {
        List<GameDTO> games = gameService.getGamesByGenre(genre);
        return ResponseEntity.ok(games);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update game")
    public ResponseEntity<GameDTO> updateGame(
            @PathVariable Long id,
            @Valid @RequestBody GameDTO gameDTO) {
        GameDTO updated = gameService.updateGame(id, gameDTO);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Activate game")
    public ResponseEntity<GameDTO> activateGame(@PathVariable Long id) {
        GameDTO game = gameService.activateGame(id);
        return ResponseEntity.ok(game);
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate game")
    public ResponseEntity<GameDTO> deactivateGame(@PathVariable Long id) {
        GameDTO game = gameService.deactivateGame(id);
        return ResponseEntity.ok(game);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete game")
    public ResponseEntity<Void> deleteGame(@PathVariable Long id) {
        gameService.deleteGame(id);
        return ResponseEntity.noContent().build();
    }

    

    @GetMapping("/stats")
    @Operation(summary = "Get game statistics")
    public ResponseEntity<Map<String, Object>> getGameStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalGames", gameService.getTotalGameCount());
        stats.put("activeGames", gameService.getActiveGameCount());
        stats.put("inactiveGames", gameService.getTotalGameCount() - gameService.getActiveGameCount());
        return ResponseEntity.ok(stats);
    }
}
