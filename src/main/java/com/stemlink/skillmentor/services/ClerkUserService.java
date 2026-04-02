package com.stemlink.skillmentor.services;

import com.stemlink.skillmentor.services.impl.dto.ClerkUserDetails;
import com.stemlink.skillmentor.services.impl.dto.ClerkCreatedUserDetails;

import java.util.Optional;

public interface ClerkUserService {
    Optional<ClerkUserDetails> getUserById(String clerkUserId);
    boolean updateUserPublicRole(String clerkUserId, String role);
    ClerkCreatedUserDetails createMentorUser(String email, String firstName, String lastName);
    boolean deleteUser(String clerkUserId);
}
