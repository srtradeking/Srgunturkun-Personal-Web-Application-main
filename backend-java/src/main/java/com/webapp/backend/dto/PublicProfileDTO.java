package com.webapp.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for public profile information
 * Contains only public user profile details without sensitive information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicProfileDTO {
    
    private Long id;
    
    private Long userId;
    
    private String displayName;
    
    private String bio;
    
    private String profilePictureUrl;
    
    private String bannerUrl;
    
    private Integer followersCount;
    
    private Integer followingCount;
    
    private Boolean isVerified;
}
