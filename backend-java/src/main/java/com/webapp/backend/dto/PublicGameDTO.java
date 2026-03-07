package com.webapp.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for public game information
 * Contains only public game details without internal metadata
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicGameDTO {
    
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
