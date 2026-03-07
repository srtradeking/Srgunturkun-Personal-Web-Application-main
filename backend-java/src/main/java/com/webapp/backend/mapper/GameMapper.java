package com.webapp.backend.mapper;

import com.webapp.backend.dto.GameDTO;
import com.webapp.backend.model.Game;
import org.mapstruct.*;

import java.util.List;

/**
 * Game Mapper
 * 
 * Maps between Game entity and GameDTO using MapStruct.
 * Provides automatic mapping with custom configurations.
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface GameMapper {

    /**
     * Convert Game entity to GameDTO
     */
    GameDTO toDTO(Game game);

    /**
     * Convert GameDTO to Game entity
     */
    Game toEntity(GameDTO gameDTO);

    /**
     * Convert list of Game entities to list of GameDTOs
     */
    List<GameDTO> toDTOList(List<Game> games);

    /**
     * Convert list of GameDTOs to list of Game entities
     */
    List<Game> toEntityList(List<GameDTO> gameDTOs);

    /**
     * Update existing Game entity from GameDTO
     * Ignores null values in DTO
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateGameFromDTO(GameDTO gameDTO, @MappingTarget Game game);
}
