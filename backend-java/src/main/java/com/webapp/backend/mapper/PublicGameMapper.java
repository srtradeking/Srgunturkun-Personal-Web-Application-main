package com.webapp.backend.mapper;

import com.webapp.backend.dto.PublicGameDTO;
import com.webapp.backend.model.Game;
import org.mapstruct.*;

import java.util.List;

/**
 * Mapper for converting Game entities to PublicGameDTOs
 * For public endpoints that expose game information
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PublicGameMapper {
    
    /**
     * Convert Game entity to PublicGameDTO
     * Only includes public information
     */
    PublicGameDTO toPublicDTO(Game game);
    
    /**
     * Convert list of Game entities to list of PublicGameDTOs
     */
    List<PublicGameDTO> toPublicDTOList(List<Game> games);
}