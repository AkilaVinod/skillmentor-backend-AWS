package com.stemlink.skillmentor.configs;

import com.stemlink.skillmentor.Repositories.UserProfileRepository;
import com.stemlink.skillmentor.constants.AppUserRole;
import com.stemlink.skillmentor.entities.UserProfile;
import com.stemlink.skillmentor.services.ClerkUserService;
import com.stemlink.skillmentor.services.impl.dto.ClerkUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Optional;

@Configuration
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DevAdminBootstrap {

    private final ClerkUserService clerkUserService;
    private final UserProfileRepository userProfileRepository;

    @Value("${app.dev.bootstrap-admin-user-id:}")
    private String adminUserId;

    @Value("${app.dev.bootstrap-admin-email:}")
    private String adminEmail;

    @Value("${app.dev.bootstrap-admin-first-name:Admin}")
    private String adminFirstName;

    @Value("${app.dev.bootstrap-admin-last-name:User}")
    private String adminLastName;

    @Bean
    public CommandLineRunner bootstrapAdminUser() {
        return args -> {
            if (adminUserId == null || adminUserId.isBlank()) {
                log.info("No dev bootstrap admin configured; skipping admin bootstrap");
                return;
            }

            boolean metadataUpdated = clerkUserService.updateUserPublicRole(adminUserId, "admin");
            if (!metadataUpdated) {
                log.warn("Failed to update Clerk public metadata for bootstrap admin {}", adminUserId);
            }

            Optional<ClerkUserDetails> clerkUser = clerkUserService.getUserById(adminUserId);
            String email = clerkUser.map(ClerkUserDetails::email).filter(value -> value != null && !value.isBlank()).orElse(adminEmail);
            String firstName = clerkUser.map(ClerkUserDetails::firstName).filter(value -> value != null && !value.isBlank()).orElse(adminFirstName);
            String lastName = clerkUser.map(ClerkUserDetails::lastName).filter(value -> value != null && !value.isBlank()).orElse(adminLastName);

            if (email == null || email.isBlank()) {
                log.warn("Cannot create bootstrap admin profile for {} because email is missing", adminUserId);
                return;
            }

            UserProfile profile = userProfileRepository.findByClerkUserId(adminUserId)
                    .orElseGet(UserProfile::new);

            profile.setClerkUserId(adminUserId);
            profile.setEmail(email);
            profile.setFirstName(firstName);
            profile.setLastName(lastName);
            profile.setRole(AppUserRole.ADMIN);
            userProfileRepository.save(profile);

            log.info("Bootstrapped local ADMIN profile for {}", adminUserId);
        };
    }
}
