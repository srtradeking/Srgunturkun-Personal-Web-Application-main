package com.webapp.backend.service;

/**
 * Account-level operations that span multiple services (profiles, posts, comments, etc.)
 */
public interface AccountService {
    /**
     * Delete an account and all associated data (posts, comments, profile) in an atomic operation.
     * @param userId numeric id (as string)
     */
    void deleteAccount(String userId);
}
