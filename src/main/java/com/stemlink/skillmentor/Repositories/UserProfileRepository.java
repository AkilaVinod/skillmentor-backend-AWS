package com.stemlink.skillmentor.Repositories;

import com.stemlink.skillmentor.entities.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findByClerkUserId(String clerkUserId);
    boolean existsByClerkUserId(String clerkUserId);
}
