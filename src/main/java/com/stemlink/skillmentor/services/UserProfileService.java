package com.stemlink.skillmentor.services;

import com.stemlink.skillmentor.constants.AppUserRole;
import com.stemlink.skillmentor.entities.UserProfile;

import java.util.Optional;

public interface UserProfileService {
    Optional<UserProfile> findByClerkUserId(String clerkUserId);

    UserProfile getOrCreateProfile(String clerkUserId);

    UserProfile setupUser(String clerkUserId, AppUserRole role);
}
