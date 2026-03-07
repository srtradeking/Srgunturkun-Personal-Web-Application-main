package com.webapp.backend.mapper;

import com.webapp.backend.dto.NotificationsDTO;
import com.webapp.backend.model.Notification;
import org.mapstruct.*;

import java.util.List;

/**
 * Notification Mapper
 *
 * Maps between Notification entity and NotificationsDTO using MapStruct.
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface NotificationMapper {

	@Mapping(target = "userProfileId", source = "userProfile.id")
	@Mapping(target = "postId", source = "post.id")
	@Mapping(target = "commentId", source = "comment.id")
	@Mapping(target = "metadata", ignore = true)
	NotificationsDTO toDto(Notification notification);

	@Mapping(target = "userProfile.id", source = "userProfileId")
	@Mapping(target = "post.id", source = "postId")
	@Mapping(target = "comment.id", source = "commentId")
	@Mapping(target = "metadata", ignore = true)
	Notification toEntity(NotificationsDTO dto);

	List<NotificationsDTO> toDtoList(List<Notification> notifications);

	List<Notification> toEntityList(List<NotificationsDTO> notificationDTOs);
}
