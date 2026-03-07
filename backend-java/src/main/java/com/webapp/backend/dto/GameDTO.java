package com.webapp.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for Game entity
 * Maps to public.games table in PostgreSQL
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameDTO {
    private Long id;
    private LocalDateTime created_at;
    private String created_by;
    private String description;
    private String genre;
    private Boolean isActive;
    private String name;
    private String platform;
    private LocalDateTime updated_at;
}

