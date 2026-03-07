package com.webapp.backend.controller;

import com.webapp.backend.dto.UserProfilesDTO;
import com.webapp.backend.service.ProfileService;
import io.jsonwebtoken.Claims;
import com.webapp.backend.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Profile REST Controller
 * 
 * Presentation Layer for Profile API endpoints.
 * Handles HTTP requests and responses for user profiles.
 */
@RestController
@RequestMapping("/profiles")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Profiles", description = "User profile management APIs")
@CrossOrigin(origins = "*")
public class ProfileController {

    private final ProfileService profileService;
    private final AccountService accountService;

    @PostMapping
    @Operation(summary = "Create or update user profile")
    public ResponseEntity<UserProfilesDTO> saveProfile(@Valid @RequestBody UserProfilesDTO profileDTO, @RequestAttribute(name = "claims", required = false) Claims claims) {
        UserProfilesDTO saved = profileService.saveProfile(profileDTO, claims);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get profile by user ID")
    public ResponseEntity<UserProfilesDTO> getProfileByUserId(@PathVariable String userId) {
        UserProfilesDTO profile = profileService.getProfileByUserId(userId, false);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/search")
    @Operation(summary = "Search profiles by display name")
    public ResponseEntity<List<UserProfilesDTO>> searchProfiles(@RequestParam String displayName) {
        List<UserProfilesDTO> profiles = profileService.searchProfilesByDisplayName(displayName);
        return ResponseEntity.ok(profiles);
    }

    @PutMapping("/{userId}")
    @Operation(summary = "Update profile")
    public ResponseEntity<UserProfilesDTO> updateProfile(@PathVariable String userId, @RequestBody UserProfilesDTO profileDTO) {
        UserProfilesDTO updated = profileService.updateProfile(userId, profileDTO);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{userId}/bio")
    @Operation(summary = "Update profile bio")
    public ResponseEntity<UserProfilesDTO> updateProfileBio(@PathVariable String userId, @RequestBody java.util.Map<String, Object> body) {
        String bio = body != null && body.get("bio") != null ? body.get("bio").toString() : null;
        UserProfilesDTO updated = profileService.updateProfileBio(userId, bio);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{userId}/photo")
    @Operation(summary = "Update profile photo (pp_url)")
    public ResponseEntity<UserProfilesDTO> updateProfilePhoto(@PathVariable String userId, @RequestBody java.util.Map<String, Object> body) {
        String photoUrl = body != null && body.get("photoUrl") != null ? body.get("photoUrl").toString() : null;
        
        // Trigger cascade update of pp_url across posts and comments
        profileService.cascadeUpdatePpUrl(userId, photoUrl);
        
        // Return updated profile
        UserProfilesDTO updated = profileService.getProfileByUserId(userId, false);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Delete profile")
    public ResponseEntity<Void> deleteProfile(@PathVariable String userId) {
        profileService.deleteProfile(userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}/cascade")
    @Operation(summary = "Delete account and all related data (posts, comments, profile) atomically")
    public ResponseEntity<Void> deleteAccountCascade(@PathVariable String userId) {
        accountService.deleteAccount(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}/exists")
    @Operation(summary = "Check if profile exists")
    public ResponseEntity<Boolean> profileExists(@PathVariable String userId) {
        boolean exists = profileService.profileExists(userId);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/count")
    @Operation(summary = "Get total profiles count")
    public ResponseEntity<Long> getTotalProfilesCount() {
        long count = profileService.getTotalProfilesCount();
        return ResponseEntity.ok(count);
    }
}