package com.stemlink.skillmentor.controllers;

import com.stemlink.skillmentor.constants.AppUserRole;
import com.stemlink.skillmentor.dto.request.UserSetupRequestDTO;
import com.stemlink.skillmentor.dto.response.UserProfileResponseDTO;
import com.stemlink.skillmentor.entities.UserProfile;
import com.stemlink.skillmentor.exceptions.SkillMentorException;
import com.stemlink.skillmentor.security.UserPrincipal;
import com.stemlink.skillmentor.services.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController extends AbstractController {

    private final UserProfileService userProfileService;

    /**
     * Returns the current user's persisted profile.
     * Users without a local record are created on demand from Clerk:
     * admins become ADMIN, mentors become MENTOR, everyone else becomes STUDENT.
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponseDTO> getMe(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UserProfile profile = userProfileService.getOrCreateProfile(principal.getId());
        return sendOkResponse(toDTO(profile));
    }

    /**
     * One-time setup: save the chosen role (STUDENT or MENTOR) plus
     * Clerk-provided user identity fields to the DB, then create the
     * corresponding Student or Mentor entity.
     */
    @PostMapping("/setup")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponseDTO> setup(
            @Valid @RequestBody UserSetupRequestDTO request,
            Authentication authentication) {

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        AppUserRole role;
        try {
            role = AppUserRole.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new SkillMentorException("Invalid role: " + request.getRole(), HttpStatus.BAD_REQUEST);
        }

        if (role == AppUserRole.ADMIN) {
            throw new SkillMentorException("ADMIN role cannot be self-assigned", HttpStatus.FORBIDDEN);
        }

        UserProfile profile = userProfileService.setupUser(
                principal.getId(),
                role
        );

        return sendCreatedResponse(toDTO(profile));
    }

    private UserProfileResponseDTO toDTO(UserProfile profile) {
        UserProfileResponseDTO dto = new UserProfileResponseDTO();
        dto.setId(profile.getId());
        dto.setClerkUserId(profile.getClerkUserId());
        dto.setEmail(profile.getEmail());
        dto.setFirstName(profile.getFirstName());
        dto.setLastName(profile.getLastName());
        dto.setRole(profile.getRole().name());
        dto.setNewUser(false);
        dto.setCreatedAt(profile.getCreatedAt());
        return dto;
    }
}
