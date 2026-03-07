package com.webapp.backend.mapper;

import com.webapp.backend.dto.UserProfilesDTO;
import com.webapp.backend.model.UserProfile;
import org.mapstruct.*;

import java.util.List;

/**
 * Profile Mapper
 *
 * Maps between UserProfile entity and UserProfilesDTO using MapStruct.
 * Provides automatic mapping with custom configurations.
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ProfileMapper {

    UserProfilesDTO toDto(UserProfile profile);

    UserProfile toEntity(UserProfilesDTO profileDTO);

    List<UserProfilesDTO> toDtoList(List<UserProfile> profiles);

    List<UserProfile> toEntityList(List<UserProfilesDTO> profileDTOs);
}