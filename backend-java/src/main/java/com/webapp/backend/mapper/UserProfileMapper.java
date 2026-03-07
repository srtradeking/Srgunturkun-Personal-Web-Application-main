package com.webapp.backend.mapper;

import com.webapp.backend.dto.UserProfilesDTO;
import com.webapp.backend.model.UserProfile;
import org.mapstruct.*;

import java.util.List;

/**
 * UserProfile Mapper
 *
 * Maps between UserProfile entity and UserProfilesDTO using MapStruct.
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UserProfileMapper {

    UserProfilesDTO toDto(UserProfile userProfile);

    UserProfile toEntity(UserProfilesDTO userProfilesDTO);

    List<UserProfilesDTO> toDtoList(List<UserProfile> userProfiles);

    List<UserProfile> toEntityList(List<UserProfilesDTO> userProfilesDTOs);
}
