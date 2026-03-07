package com.webapp.backend.mapper;

import com.webapp.backend.dto.CommentsDTO;
import com.webapp.backend.model.Comment;
import org.mapstruct.*;

import java.util.List;

/**
 * Comment Mapper
 *
 * Maps between Comment entity and CommentsDTO using MapStruct.
 * Handles mapping of new fields: userProfileId, engagement metrics, soft delete flags
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface CommentMapper {

    @Mapping(target = "userProfileId", source = "userProfile.id")
    @Mapping(target = "content", source = "content")
    @Mapping(target = "parentCommentId", source = "parentComment.id")
    CommentsDTO toDto(Comment comment);

    @Mapping(target = "userProfile.id", source = "userProfileId")
    @Mapping(target = "content", source = "content")
    Comment toEntity(CommentsDTO commentDTO);

    List<CommentsDTO> toDtoList(List<Comment> comments);

    List<Comment> toEntityList(List<CommentsDTO> commentDTOs);
}