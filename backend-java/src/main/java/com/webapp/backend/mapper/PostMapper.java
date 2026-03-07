package com.webapp.backend.mapper;

import com.webapp.backend.dto.PostDTO;
import com.webapp.backend.model.Post;
import org.mapstruct.*;

import java.util.List;

/**
 * Post Mapper
 *
 * Maps between Post entity and PostDTO using MapStruct.
 * Handles conversion between Post entity (database model) and PostDTO (API model)
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL
)
public interface PostMapper {

    @Mapping(target = "userProfileId", source = "userProfile.id")
    @Mapping(target = "userId", source = "userProfile.id")
    @Mapping(target = "type", expression = "java(calculatePostType(post))")
    @Mapping(target = "url", expression = "java(calculatePostUrl(post))")
    @Mapping(target = "mimeType", expression = "java(calculateMimeType(post))")
    PostDTO toDto(Post post);

    @Mapping(target = "userProfile.id", source = "userProfileId")
    Post toEntity(PostDTO postDTO);

    List<PostDTO> toDtoList(List<Post> posts);

    List<Post> toEntityList(List<PostDTO> postDTOs);

    default String calculatePostType(Post post) {
        if (post.getVideoUrl() != null && !post.getVideoUrl().isEmpty()) {
            return "video";
        } else if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            return "image";
        }
        return "text";
    }

    default String calculatePostUrl(Post post) {
        if (post.getVideoUrl() != null && !post.getVideoUrl().isEmpty()) {
            return post.getVideoUrl();
        } else if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            return post.getImageUrl();
        }
        return null;
    }

    default String calculateMimeType(Post post) {
        if (post.getVideoUrl() != null && !post.getVideoUrl().isEmpty()) {
            return "video/mp4";
        } else if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            return "image/jpeg";
        }
        return null;
    }
}
