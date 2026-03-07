package com.webapp.backend.mapper;

import com.webapp.backend.dto.VideoMetadataDTO;
import com.webapp.backend.model.VideoMetadata;
import org.mapstruct.*;
import java.util.List;

/**
 * VideoMetadata Mapper
 * Maps between VideoMetadata entity and VideoMetadataDTO
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL
)
public interface VideoMetadataMapper {

    VideoMetadataDTO toDto(VideoMetadata videoMetadata);

    VideoMetadata toEntity(VideoMetadataDTO videoMetadataDTO);

    List<VideoMetadataDTO> toDtoList(List<VideoMetadata> videos);

    List<VideoMetadata> toEntityList(List<VideoMetadataDTO> videoDTOs);
}
