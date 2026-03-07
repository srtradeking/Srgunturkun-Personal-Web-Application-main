package com.webapp.backend.repository;

import com.webapp.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    /**
     * Delete the application credential record that is linked to the given
     * UserProfile primary key. This prevents foreign key violations when the
     * profile is deleted as part of an account cascade.
     */
    void deleteByUserProfile_Id(Long userProfileId);

    Optional<User> findByUserProfile_Id(Long userProfileId);
}
