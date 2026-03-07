package com.webapp.backend.service;

import java.util.List;

import com.webapp.backend.dto.UserProfilesDTO;

/**
 * Profile Service Interface
 * 
 * Business Logic Layer interface for Profile operations.
 * Handles user profile business logic and data access.
 */
public interface ProfileService {
    
    /**
     * Create or update user profile
     */
    UserProfilesDTO saveProfile(UserProfilesDTO profileDTO, io.jsonwebtoken.Claims claims);
    
    /**
     * Get profile by user ID
     */
    UserProfilesDTO getProfileByUserId(String userId, boolean isPublic);
    
    /**
     * Search profiles by display name
     */
    List<UserProfilesDTO> searchProfilesByDisplayName(String displayName);
    
    /**
     * Update profile
     */
    UserProfilesDTO updateProfile(String userId, UserProfilesDTO profileDTO);
    
    /**
     * Delete profile
     */
    void deleteProfile(String userId);
    
    /**
     * Check if profile exists
     */
    boolean profileExists(String userId);
    
    /**
     * Update profile bio
     */
    UserProfilesDTO updateProfileBio(String userId, String bio);
    
    /**
     * Get total profiles count
     */
    long getTotalProfilesCount();
    
    /**
     * Cascade update profile_picture_url across user profile
     */
    void cascadeUpdatePpUrl(String userId, String newPpUrl);
}