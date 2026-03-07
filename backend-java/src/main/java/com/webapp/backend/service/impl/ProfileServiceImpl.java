package com.webapp.backend.service.impl;

import com.webapp.backend.dto.UserProfilesDTO;
import com.webapp.backend.model.UserProfile;
import com.webapp.backend.repository.CommentRepository;
import com.webapp.backend.repository.PostRepository;
import com.webapp.backend.repository.UserProfileRepository;
import io.jsonwebtoken.Claims;
import com.webapp.backend.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
// StandardCharsets no longer needed after switching ppUrl/photoURL to String
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Profile Service Implementation backed by JPA repository.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileServiceImpl implements ProfileService {

    private final UserProfileRepository userProfileRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    private UserProfilesDTO toDto(UserProfile entity) {
        if (entity == null) return null;
        return UserProfilesDTO.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .displayName(entity.getDisplayName())
                .email(entity.getEmail())
                .bio(entity.getBio())
                .profilePictureUrl(entity.getProfilePictureUrl())
                .isActive(entity.getIsActive())
                .isVerified(entity.getIsVerified())
                .isBanned(entity.getIsBanned())
                .banReason(entity.getBanReason())
                .bannedAt(entity.getBannedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .lastLoginAt(entity.getLastLoginAt())
                .totalLikesPoint(entity.getTotalLikesPoint())
                .build();
    }

    private UserProfile toEntity(UserProfilesDTO dto, UserProfile existing) {
        UserProfile e = existing != null ? existing : new UserProfile();

        // Only update fields that are provided in the DTO (partial updates supported)
        if (dto.getUsername() != null) e.setUsername(dto.getUsername());
        if (dto.getDisplayName() != null) e.setDisplayName(dto.getDisplayName());
        if (dto.getEmail() != null) e.setEmail(dto.getEmail());
        if (dto.getBio() != null) e.setBio(dto.getBio());
        if (dto.getProfilePictureUrl() != null) e.setProfilePictureUrl(dto.getProfilePictureUrl());
        if (dto.getIsActive() != null) e.setIsActive(dto.getIsActive());
        if (dto.getIsVerified() != null) e.setIsVerified(dto.getIsVerified());
        if (dto.getIsBanned() != null) e.setIsBanned(dto.getIsBanned());
        if (dto.getBanReason() != null) e.setBanReason(dto.getBanReason());
        if (dto.getBannedAt() != null) e.setBannedAt(dto.getBannedAt());
        if (dto.getLastLoginAt() != null) e.setLastLoginAt(dto.getLastLoginAt());
        
        return e;
    }

    /**
     * Get numeric user ID (database primary key) from either numeric ID
     */
    private Long getNumericUserId(String userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        // If userId is already numeric, parse and return it
        if (userId.matches("\\d+")) {
            return Long.parseLong(userId);
        }
        
        // Otherwise, it's a username
        UserProfile profile = userProfileRepository.findByUsername(userId)
                .orElseThrow(() -> new com.webapp.backend.exception.ResourceNotFoundException(
                        "Profile not found for username: " + userId));
        return profile.getId();
    }

    @Override
    public UserProfilesDTO saveProfile(UserProfilesDTO profileDTO, Claims claims) {
        log.info("Saving profile for user: {}", profileDTO.getUsername());

        // Try to find existing by username or numeric id
        UserProfile existing = null;
        String username = profileDTO.getUsername();
        if (username != null) {
            Optional<UserProfile> byUsername = userProfileRepository.findByUsername(username);
            if (byUsername.isPresent()) {
                existing = byUsername.get();
                log.info("Found existing profile for username: {}", username);
            }
        } else if (profileDTO.getId() != null) {
            Optional<UserProfile> byId = userProfileRepository.findById(profileDTO.getId());
            if (byId.isPresent()) {
                existing = byId.get();
                log.info("Found existing profile for id: {}", profileDTO.getId());
            }
        }

        // Check for duplicate email if creating new profile
        if (existing == null && profileDTO.getEmail() != null) {
            Optional<UserProfile> byEmail = userProfileRepository.findByEmail(profileDTO.getEmail());
            if (byEmail.isPresent()) {
                log.warn("Profile with email {} already exists", profileDTO.getEmail());
                throw new com.webapp.backend.exception.DuplicateResourceException(
                    "A profile with this email already exists: " + profileDTO.getEmail());
            }
        }

        // If creating a new profile, always generate and set a sanitized username.
        if (existing == null) {
            String generatedUsername = generateUsername(profileDTO);
            profileDTO.setUsername(generatedUsername);
            log.info("Generated sanitized username: {} for user: {}", generatedUsername, profileDTO.getUsername());

            // Check for email_verified claim
            if (claims != null && claims.get("email_verified", Boolean.class) == Boolean.TRUE) {
                profileDTO.setIsVerified(true);
            }
        }

        UserProfile toSave = toEntity(profileDTO, existing);
        UserProfile saved = userProfileRepository.save(toSave);
        return toDto(saved);
    }

    private String generateUsername(UserProfilesDTO profileDTO) {
        String baseUsername;
        
        if (profileDTO.getDisplayName() != null && !profileDTO.getDisplayName().isEmpty()) {
            baseUsername = profileDTO.getDisplayName().toLowerCase().replaceAll("[^a-z0-9]", "");
        } else if (profileDTO.getEmail() != null) {
            baseUsername = profileDTO.getEmail().split("@")[0].toLowerCase().replaceAll("[^a-z0-9]", "");
        } else {
            baseUsername = "user_" + System.currentTimeMillis();
        }
        
        if (baseUsername.length() > 50) {
            baseUsername = baseUsername.substring(0, 50);
        }
        
        String username = baseUsername;
        int counter = 1;
        
        while (userProfileRepository.findByUsername(username).isPresent()) {
            username = baseUsername + "_" + counter;
            counter++;
            if (counter > 1000) {
                username = baseUsername + "_" + System.currentTimeMillis();
                break;
            }
        }
        
        return username;
    }

    @Override
    public UserProfilesDTO getProfileByUserId(String userId, boolean isPublic) {
        log.info("Fetching profile by user ID: {}", userId);
        UserProfile found = null;
        if (userId.matches("\\d+")) {
            try {
                Long id = Long.parseLong(userId);
                found = userProfileRepository.findById(id).orElse(null);
            } catch (NumberFormatException ignored) {}
        }
        if (found == null) throw new com.webapp.backend.exception.ResourceNotFoundException("Profile not found for user: " + userId);

        if (!isPublic && !found.getIsVerified()) {
            throw new com.webapp.backend.exception.NotAuthorizedException("User email not verified");
        }

        return toDto(found);
    }

    @Override
    public List<UserProfilesDTO> searchProfilesByDisplayName(String displayName) {
        // Simple implementation: fetch all and filter
        return userProfileRepository.findAll().stream()
                .filter(u -> u.getDisplayName() != null && u.getDisplayName().toLowerCase().contains(displayName.toLowerCase()))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public UserProfilesDTO updateProfile(String userId, UserProfilesDTO profileDTO) {
        UserProfilesDTO existing = getProfileByUserId(userId, false);
        UserProfile entity = userProfileRepository.findById(existing.getId()).orElse(null);
        if (entity == null && existing.getId() != null) {
            entity = userProfileRepository.findById(existing.getId()).orElse(null);
        }
        UserProfile updated = toEntity(profileDTO, entity);
        UserProfile saved = userProfileRepository.save(updated);
        return toDto(saved);
    }

    @Override
    public UserProfilesDTO updateProfileBio(String userId, String bio) {
        UserProfile entity;
        if (userId.matches("\\d+")) {
            entity = userProfileRepository.findById(Long.parseLong(userId)).orElse(null);
        } else {
            entity = userProfileRepository.findByUsername(userId).orElse(null);
        }
    if (entity == null) throw new com.webapp.backend.exception.ResourceNotFoundException("Profile not found for user: " + userId);
        entity.setBio(bio);
        entity.setUpdatedAt(LocalDateTime.now());
        return toDto(userProfileRepository.save(entity));
    }

    @Override
    public void deleteProfile(String userId) {
        if (userId.matches("\\d+")) {
            userProfileRepository.deleteById(Long.parseLong(userId));
        } else {
            Optional<UserProfile> found = userProfileRepository.findByUsername(userId);
            found.ifPresent(userProfileRepository::delete);
        }
    }

    @Override
    public boolean profileExists(String userId) {
        if (userId.matches("\\d+")) {
            return userProfileRepository.existsById(Long.parseLong(userId));
        }
        return userProfileRepository.findByUsername(userId).isPresent();
    }

    @Override
    public long getTotalProfilesCount() {
        return userProfileRepository.count();
    }

    @Override
    @Transactional
    public void cascadeUpdatePpUrl(String userId, String newPpUrl) {
        log.info("Starting cascade update of profilePictureUrl for userId: {}", userId);
        
        // Get numeric user ID using helper method (handles numeric ID)
        Long numericUserId = getNumericUserId(userId);
        log.debug("Resolved userId {} to numeric ID: {}", userId, numericUserId);
        
        // Step 1: Update user profile
        UserProfile profile = userProfileRepository.findById(numericUserId)
                .orElseThrow(() -> new com.webapp.backend.exception.ResourceNotFoundException(
                        "Profile not found for user: " + userId));
        
        profile.setProfilePictureUrl(newPpUrl);
        profile.setUpdatedAt(LocalDateTime.now());
        userProfileRepository.save(profile);
        log.debug("Updated UserProfile profilePictureUrl to: {} for userId: {}", newPpUrl, userId);
        
        // Note: Posts and Comments now have user_profile_id foreign key
        // and can be updated through database constraints
        
        log.info("Successfully completed cascade update of profilePictureUrl for userId: {}", userId);
    }
}
