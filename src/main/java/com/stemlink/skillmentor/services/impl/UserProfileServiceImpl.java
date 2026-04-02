package com.stemlink.skillmentor.services.impl;

import com.stemlink.skillmentor.Repositories.MentorRepository;
import com.stemlink.skillmentor.Repositories.StudentRepository;
import com.stemlink.skillmentor.Repositories.UserProfileRepository;
import com.stemlink.skillmentor.constants.AppUserRole;
import com.stemlink.skillmentor.entities.Mentor;
import com.stemlink.skillmentor.entities.Student;
import com.stemlink.skillmentor.entities.UserProfile;
import com.stemlink.skillmentor.exceptions.SkillMentorException;
import com.stemlink.skillmentor.services.ClerkUserService;
import com.stemlink.skillmentor.services.UserProfileService;
import com.stemlink.skillmentor.services.impl.dto.ClerkUserDetails;
import com.stemlink.skillmentor.utils.MentorAvailabilityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final StudentRepository studentRepository;
    private final MentorRepository mentorRepository;
    private final ClerkUserService clerkUserService;

    @Override
    public Optional<UserProfile> findByClerkUserId(String clerkUserId) {
        return userProfileRepository.findByClerkUserId(clerkUserId);
    }

    @Override
    @Transactional
    public UserProfile getOrCreateProfile(String clerkUserId) {
        ClerkUserDetails clerkUser = clerkUserService.getUserById(clerkUserId)
                .orElseThrow(() -> new SkillMentorException(
                        "Unable to load user details from Clerk. Check CLERK_SECRET_KEY configuration.",
                        HttpStatus.BAD_GATEWAY
                ));

        String normalizedEmail = normalizeRequired(clerkUser.email(), "Email");
        String normalizedFirstName = normalizeOptional(clerkUser.firstName());
        String normalizedLastName = normalizeOptional(clerkUser.lastName());
        UserProfile profile = userProfileRepository.findByClerkUserId(clerkUserId)
                .orElseGet(UserProfile::new);
        AppUserRole resolvedRole = resolveRole(clerkUser, profile.getRole());

        profile.setClerkUserId(clerkUserId);
        profile.setEmail(normalizedEmail);
        profile.setFirstName(normalizedFirstName);
        profile.setLastName(normalizedLastName);
        profile.setRole(resolvedRole);

        UserProfile savedProfile = userProfileRepository.save(profile);
        syncDomainRecord(savedProfile);
        return savedProfile;
    }

    @Override
    @Transactional
    public UserProfile setupUser(String clerkUserId, AppUserRole role) {
        if (userProfileRepository.existsByClerkUserId(clerkUserId)) {
            throw new SkillMentorException("User profile already exists", HttpStatus.CONFLICT);
        }

        ClerkUserDetails clerkUser = clerkUserService.getUserById(clerkUserId)
                .orElseThrow(() -> new SkillMentorException(
                        "Unable to load user details from Clerk. Check CLERK_SECRET_KEY configuration.",
                        HttpStatus.BAD_GATEWAY
                ));

        String normalizedEmail = normalizeRequired(clerkUser.email(), "Email");
        String normalizedFirstName = normalizeOptional(clerkUser.firstName());
        String normalizedLastName = normalizeOptional(clerkUser.lastName());

        UserProfile profile = new UserProfile();
        profile.setClerkUserId(clerkUserId);
        profile.setEmail(normalizedEmail);
        profile.setFirstName(normalizedFirstName);
        profile.setLastName(normalizedLastName);
        profile.setRole(role);
        UserProfile savedProfile = userProfileRepository.save(profile);
        syncDomainRecord(savedProfile);
        return savedProfile;
    }

    private AppUserRole resolveRole(ClerkUserDetails clerkUser, AppUserRole existingRole) {
        String role = normalizeOptional(clerkUser.role()).toUpperCase();
        if ("ADMIN".equals(role)) {
            return AppUserRole.ADMIN;
        }
        if ("MENTOR".equals(role)) {
            return AppUserRole.MENTOR;
        }
        if (existingRole == AppUserRole.ADMIN || existingRole == AppUserRole.MENTOR) {
            return existingRole;
        }
        return AppUserRole.STUDENT;
    }

    private void syncDomainRecord(UserProfile profile) {
        if (profile.getRole() == AppUserRole.STUDENT) {
            Student student = studentRepository.findByStudentId(profile.getClerkUserId())
                    .or(() -> studentRepository.findByEmail(profile.getEmail()))
                    .orElseGet(Student::new);
            student.setStudentId(profile.getClerkUserId());
            student.setEmail(profile.getEmail());
            student.setFirstName(profile.getFirstName());
            student.setLastName(profile.getLastName());
            studentRepository.save(student);
            return;
        }

        if (profile.getRole() == AppUserRole.MENTOR) {
            Mentor mentor = mentorRepository.findByMentorId(profile.getClerkUserId())
                    .or(() -> mentorRepository.findByEmail(profile.getEmail()))
                    .orElseGet(Mentor::new);
            mentor.setMentorId(profile.getClerkUserId());
            mentor.setEmail(profile.getEmail());
            mentor.setFirstName(profile.getFirstName());
            mentor.setLastName(profile.getLastName());
            if (mentor.getAvailabilities() == null || mentor.getAvailabilities().isEmpty()) {
                mentor.setAvailabilities(MentorAvailabilityUtils.buildDefaultWeekdayAvailability(mentor));
            }
            mentorRepository.save(mentor);
        }
    }

    private String normalizeRequired(String value, String fieldName) {
        String normalized = normalizeOptional(value);
        if (normalized.isBlank()) {
            throw new SkillMentorException(fieldName + " is required", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        return value == null ? "" : value.trim();
    }
}
