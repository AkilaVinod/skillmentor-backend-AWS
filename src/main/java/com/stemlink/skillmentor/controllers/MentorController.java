package com.stemlink.skillmentor.controllers;



import com.stemlink.skillmentor.Repositories.SessionRepository;
import com.stemlink.skillmentor.Repositories.ReviewRepository;
import com.stemlink.skillmentor.dto.request.MentorRequestDTO;
import com.stemlink.skillmentor.dto.response.MentorAvailabilityResponseDTO;
import com.stemlink.skillmentor.dto.response.MentorProvisionResponseDTO;
import com.stemlink.skillmentor.dto.response.MentorResponseDTO;
import com.stemlink.skillmentor.dto.response.SubjectResponseDTO;
import com.stemlink.skillmentor.entities.Mentor;
import com.stemlink.skillmentor.entities.Review;
import com.stemlink.skillmentor.security.UserPrincipal;
import com.stemlink.skillmentor.services.MentorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.stemlink.skillmentor.constants.UserRoles.*;

@RestController
@RequestMapping(path = "/api/v1/mentors")
@RequiredArgsConstructor
@Validated
//@PreAuthorize("isAuthenticated()") // Allow all authenticated users to access mentor endpoints, but specific actions are further restricted by method-level security annotations
public class MentorController extends AbstractController {

    private final MentorService mentorService;
    private final ModelMapper modelMapper;
    private final SessionRepository sessionRepository;
    private final ReviewRepository reviewRepository;

    @GetMapping
    public ResponseEntity<Page<MentorResponseDTO>> getAllMentors(
            @RequestParam(required = false) String name,
            Pageable pageable) {

        Page<Mentor> mentors = mentorService.getAllMentors(name, pageable);
        Page<MentorResponseDTO> dtoPage = mentors.map(mentor -> buildMentorResponseDTO(mentor, false));
        return sendOkResponse(dtoPage);
    }

    @GetMapping("{id}")
    public ResponseEntity<MentorResponseDTO> getMentorById(@PathVariable Long id) {
        Mentor mentor = mentorService.getMentorById(id);
        return sendOkResponse(buildMentorResponseDTO(mentor, true));
    }

    @GetMapping("/profile/{mentorId}")
    public ResponseEntity<MentorResponseDTO> getMentorByMentorId(@PathVariable String mentorId) {
        Mentor mentor = mentorService.getMentorByMentorId(mentorId);
        return sendOkResponse(buildMentorResponseDTO(mentor, true));
    }

    private MentorResponseDTO buildMentorResponseDTO(Mentor mentor, boolean includeSubjects) {
        MentorResponseDTO dto = modelMapper.map(mentor, MentorResponseDTO.class);

        if (includeSubjects && mentor.getSubjects() != null) {
            List<SubjectResponseDTO> subjectDTOs = mentor.getSubjects().stream().map(subject -> {
                SubjectResponseDTO s = modelMapper.map(subject, SubjectResponseDTO.class);
                s.setMentorId(mentor.getMentorId());
                s.setMentorName(mentor.getFirstName() + " " + mentor.getLastName());
                s.setEnrollmentCount(sessionRepository.countBySubjectId(subject.getId()));
                return s;
            }).collect(Collectors.toList());
            dto.setSubjects(subjectDTOs);
        } else {
            dto.setSubjects(Collections.emptyList());
        }

        List<Review> reviews = reviewRepository.findByMentorId(mentor.getId());
        int totalReviews = reviews.size();
        double averageRating = reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
        int positiveReviewPercentage = totalReviews == 0
                ? 0
                : (int) Math.round(
                (reviews.stream().filter(review -> review.getRating() >= 4).count() * 100.0) / totalReviews
        );

        dto.setAverageRating(totalReviews == 0 ? null : averageRating);
        dto.setTotalReviews(totalReviews);
        dto.setTotalEnrollments((int) sessionRepository.countByMentorId(mentor.getId()));
        dto.setTotalStudentsTaught((int) sessionRepository.countDistinctStudentsByMentorId(mentor.getId()));
        dto.setPositiveReviewPercentage(positiveReviewPercentage);
        dto.setAvailabilities(
                mentor.getAvailabilities() == null
                        ? Collections.emptyList()
                        : mentor.getAvailabilities().stream()
                        .map(availability -> modelMapper.map(availability, MentorAvailabilityResponseDTO.class))
                        .toList()
        );

        return dto;
    }

    @PostMapping
    @PreAuthorize("hasRole('" + ROLE_ADMIN + "')")
    public ResponseEntity<MentorProvisionResponseDTO> createMentor(@Valid @RequestBody MentorRequestDTO mentorRequestDTO) {
        MentorProvisionResponseDTO responseDTO = mentorService.provisionMentor(mentorRequestDTO);
        return sendCreatedResponse(responseDTO);
    }

    @PutMapping("{id}")
    @PreAuthorize("hasAnyRole('" + ROLE_ADMIN + "', '" + ROLE_MENTOR + "')")
    public ResponseEntity<MentorResponseDTO> updateMentor(
            @PathVariable Long id,
            @Valid @RequestBody MentorRequestDTO updatedMentorDTO,
            Authentication authentication) {

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        Mentor existingMentor = mentorService.getMentorById(id);

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !existingMentor.getMentorId().equals(userPrincipal.getId())) {
            throw new AccessDeniedException("You are not allowed to update this mentor");
        }

        Mentor mentor = modelMapper.map(updatedMentorDTO, Mentor.class);

        Mentor updatedMentor = mentorService.updateMentorById(id, mentor);

        MentorResponseDTO responseDTO = modelMapper.map(updatedMentor, MentorResponseDTO.class);

        return sendOkResponse(responseDTO);
    }

    @DeleteMapping("{id}")
    @PreAuthorize("hasRole('" + ROLE_ADMIN + "')")
    public ResponseEntity<Void> deleteMentor(@PathVariable Long id) {
        mentorService.deleteMentor(id);
        return sendNoContentResponse();
    }
}

