package com.stemlink.skillmentor.services.impl;

import com.stemlink.skillmentor.Repositories.MentorRepository;
import com.stemlink.skillmentor.Repositories.UserProfileRepository;
import com.stemlink.skillmentor.constants.AppUserRole;
import com.stemlink.skillmentor.dto.request.MentorRequestDTO;
import com.stemlink.skillmentor.dto.response.MentorProvisionResponseDTO;
import com.stemlink.skillmentor.dto.response.MentorResponseDTO;
import com.stemlink.skillmentor.entities.Mentor;
import com.stemlink.skillmentor.entities.UserProfile;
import com.stemlink.skillmentor.exceptions.SkillMentorException;
import com.stemlink.skillmentor.services.ClerkUserService;
import com.stemlink.skillmentor.services.MentorService;
import com.stemlink.skillmentor.services.impl.dto.ClerkCreatedUserDetails;
import com.stemlink.skillmentor.utils.MentorAvailabilityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MentorServiceImpl implements MentorService {

    private final MentorRepository mentorRepository;
    private final UserProfileRepository userProfileRepository;
    private final ClerkUserService clerkUserService;
    private final ModelMapper modelMapper;

    @CacheEvict(value = {"mentors", "mentor"}, allEntries = true)
    public Mentor createNewMentor(Mentor mentor) {
        try {
            if (mentor.getAvailabilities() == null || mentor.getAvailabilities().isEmpty()) {
                mentor.setAvailabilities(MentorAvailabilityUtils.buildDefaultWeekdayAvailability(mentor));
            }
            return mentorRepository.save(mentor);
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while creating mentor: {}", e.getMessage());
            String message = e.getMostSpecificCause() != null
                    ? e.getMostSpecificCause().getMessage()
                    : e.getMessage();

            if (message != null && message.toLowerCase().contains("email")) {
                throw new SkillMentorException("Mentor with this email already exists", HttpStatus.CONFLICT);
            }

            throw new SkillMentorException("Invalid mentor data: " + message, HttpStatus.BAD_REQUEST);
        } catch (Exception exception) {
            log.error("Failed to create new mentor", exception);
            throw new SkillMentorException("Failed to create new mentor", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @CacheEvict(value = {"mentors", "mentor"}, allEntries = true)
    public MentorProvisionResponseDTO provisionMentor(MentorRequestDTO mentorRequestDTO) {
        ClerkCreatedUserDetails createdUser = clerkUserService.createMentorUser(
                mentorRequestDTO.getEmail(),
                mentorRequestDTO.getFirstName(),
                mentorRequestDTO.getLastName()
        );

        try {
            Mentor mentor = new Mentor();
            mentor.setMentorId(createdUser.userId());
            mentor.setFirstName(mentorRequestDTO.getFirstName());
            mentor.setLastName(mentorRequestDTO.getLastName());
            mentor.setEmail(mentorRequestDTO.getEmail());
            mentor.setPhoneNumber(mentorRequestDTO.getPhoneNumber());
            mentor.setTitle(mentorRequestDTO.getTitle());
            mentor.setProfession(mentorRequestDTO.getProfession());
            mentor.setCompany(mentorRequestDTO.getCompany());
            mentor.setExperienceYears(mentorRequestDTO.getExperienceYears());
            mentor.setBio(mentorRequestDTO.getBio());
            mentor.setProfileImageUrl(mentorRequestDTO.getProfileImageUrl());
            mentor.setIsCertified(mentorRequestDTO.getIsCertified());
            mentor.setStartYear(mentorRequestDTO.getStartYear());
            Mentor createdMentor = createNewMentor(mentor);

            UserProfile userProfile = new UserProfile();
            userProfile.setClerkUserId(createdUser.userId());
            userProfile.setEmail(mentorRequestDTO.getEmail());
            userProfile.setFirstName(mentorRequestDTO.getFirstName());
            userProfile.setLastName(mentorRequestDTO.getLastName());
            userProfile.setRole(AppUserRole.MENTOR);
            userProfileRepository.save(userProfile);

            MentorProvisionResponseDTO responseDTO = new MentorProvisionResponseDTO();
            responseDTO.setMentor(modelMapper.map(createdMentor, MentorResponseDTO.class));
            responseDTO.setClerkUserId(createdUser.userId());
            responseDTO.setLoginEmail(createdUser.email());
            responseDTO.setTemporaryPassword(createdUser.temporaryPassword());
            return responseDTO;
        } catch (Exception exception) {
            clerkUserService.deleteUser(createdUser.userId());

            if (exception instanceof SkillMentorException skillMentorException) {
                throw skillMentorException;
            }

            log.error("Failed to provision mentor {}", mentorRequestDTO.getEmail(), exception);
            throw new SkillMentorException("Failed to provision mentor account", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Cacheable(value = "mentors", key = "#name + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<Mentor> getAllMentors(String name, Pageable pageable) {
        try {
            log.debug("Fetching mentors from DB with name: {}", name);

            if (name != null && !name.isEmpty()) {
                return mentorRepository.findByName(name, pageable);
            }

            return mentorRepository.findAll(pageable);

        } catch (Exception exception) {
            log.error("Failed to get all mentors", exception);
            throw new SkillMentorException("Failed to get all mentors", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Cacheable(value = "mentor", key = "#id")
    public Mentor getMentorById(Long id) {
        try {

            Mentor mentor = mentorRepository.findById(id).orElseThrow(
                    () -> new SkillMentorException("Mentor Not found", HttpStatus.NOT_FOUND)
            );

            log.info("Successfully fetched mentor {}", id);
            return mentor;

        } catch (SkillMentorException skillMentorException) {

            log.warn("Mentor not found with id: {} to fetch", id, skillMentorException);
            throw new SkillMentorException("Mentor Not found", HttpStatus.NOT_FOUND);

        } catch (Exception exception) {

            log.error("Error getting mentor", exception);
            throw new SkillMentorException("Failed to get mentor", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Cacheable(value = "mentor", key = "#mentorId")
    public Mentor getMentorByMentorId(String mentorId) {
        return mentorRepository.findByMentorId(mentorId)
                .orElseThrow(() ->
                        new SkillMentorException("Mentor not found", HttpStatus.NOT_FOUND));
    }

    @CacheEvict(value = {"mentors", "mentor"}, allEntries = true)
    public Mentor updateMentorById(Long id, Mentor updatedMentor) {
        try {

            Mentor mentor = mentorRepository.findById(id).orElseThrow(
                    () -> new SkillMentorException("Mentor Not found", HttpStatus.NOT_FOUND)
            );

            modelMapper.map(updatedMentor, mentor);

            if (mentor.getAvailabilities() == null || mentor.getAvailabilities().isEmpty()) {
                mentor.setAvailabilities(MentorAvailabilityUtils.buildDefaultWeekdayAvailability(mentor));
            }

            return mentorRepository.save(mentor);

        } catch (SkillMentorException skillMentorException) {

            log.warn("Mentor not found with id: {} to update", id, skillMentorException);
            throw new SkillMentorException("Mentor Not found", HttpStatus.NOT_FOUND);

        } catch (Exception exception) {

            log.error("Error updating mentor", exception);
            throw new SkillMentorException("Failed to update mentor", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @CacheEvict(value = {"mentors", "mentor"}, allEntries = true)
    public void deleteMentor(Long id) {
        try {

            mentorRepository.deleteById(id);

        } catch (Exception exception) {

            log.error("Failed to delete mentor with id {}", id, exception);
            throw new SkillMentorException("Failed to delete mentor", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}