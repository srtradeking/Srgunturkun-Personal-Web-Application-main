package com.webapp.backend.controller;

import com.webapp.backend.dto.UserProfilesDTO;
import com.webapp.backend.dto.PublicProfileDTO;
import com.webapp.backend.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public Profile Controller
 *
 * Exposes a read-only endpoint for retrieving non-sensitive profile information
 * that can be displayed on public pages (profile previews, search results, etc.).
 */
@RestController
@RequestMapping("/public/profiles")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class PublicProfileController {

    private final ProfileService profileService;

    @GetMapping("/{userId}")
    @Operation(summary = "Get public profile by user ID")
    public ResponseEntity<PublicProfileDTO> getPublicProfile(@PathVariable String userId) {
        log.info("REST request to get public profile for userId={}", userId);
        UserProfilesDTO profile = profileService.getProfileByUserId(userId, true);
        if (profile == null) {
            return ResponseEntity.notFound().build();
        }

        PublicProfileDTO publicProfile = PublicProfileDTO.builder()
                .id(profile.getId())
                .displayName(profile.getDisplayName())
                .bio(profile.getBio())
                .profilePictureUrl(profile.getProfilePictureUrl())
                .build();

        return ResponseEntity.ok(publicProfile);
    }
}
