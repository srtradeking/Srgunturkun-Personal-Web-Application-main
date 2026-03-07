package com.webapp.backend.service.impl;

import com.webapp.backend.service.AccountService;
import com.webapp.backend.service.CommentService;
import com.webapp.backend.service.PostService;
import com.webapp.backend.service.ProfileService;
import com.webapp.backend.repository.UserProfileRepository;
import com.webapp.backend.repository.UserRepository;
import com.webapp.backend.repository.EmailVerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {

    private final ProfileService profileService;
    private final PostService postService;
    private final CommentService commentService;
    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;

    /**
     * Delete account and all related data atomically. This method runs inside a transaction so
     * either all related records are removed or none are.
     */
    @Override
    @Transactional
    public void deleteAccount(String userId) {
        log.info("Account-level delete requested for userId={}", userId);

        Long numericId = null;
        if (userId != null && userId.matches("\\d+")) {
            try { numericId = Long.parseLong(userId); } catch (NumberFormatException ignored) {}
        }

        // If we have a numeric id (this is the user_profile.id for this flow),
        // delete comments by user, posts (and their comments), and the linked
        // credential record in private.users before we remove the profile.
        if (numericId != null) {
            try {
                commentService.deleteCommentsByUserId(numericId);
            } catch (Exception e) {
                log.warn("Failed to delete comments by user {}: {}", numericId, e.getMessage());
                throw e;
            }

            try {
                postService.deletePostsByUserId(numericId);
            } catch (Exception e) {
                log.warn("Failed to delete posts by user {}: {}", numericId, e.getMessage());
                throw e;
            }

            // Remove the application user row that still references this profile
            // via users.user_profile_id FK, to avoid constraint violations when
            // the profile row is deleted.
            try {
                userRepository.deleteByUserProfile_Id(numericId);
            } catch (Exception e) {
                log.warn("Failed to delete credential user linked to profile {}: {}", numericId, e.getMessage());
                throw e;
            }

            // Also remove any email verification tokens linked to this profile
            try {
                emailVerificationTokenRepository.deleteByUserProfile_Id(numericId);
            } catch (Exception e) {
                log.warn("Failed to delete email verification tokens linked to profile {}: {}", numericId, e.getMessage());
                throw e;
            }
        }

        // Finally delete the profile record for the given user identifier
        try {
            profileService.deleteProfile(userId);
        } catch (Exception e) {
            log.warn("Failed to delete profile for user {}: {}", userId, e.getMessage());
            throw e;
        }

        log.info("Account deletion completed for userId={}", userId);
    }
}
