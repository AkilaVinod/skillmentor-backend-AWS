package com.stemlink.skillmentor.controllers;

import com.stemlink.skillmentor.dto.request.SessionRequestDTO;
import com.stemlink.skillmentor.dto.response.SessionResponseDTO;
import com.stemlink.skillmentor.entities.Session;
import com.stemlink.skillmentor.exceptions.SkillMentorException;
import com.stemlink.skillmentor.security.UserPrincipal;
import com.stemlink.skillmentor.services.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = "/api/v1/sessions")
@RequiredArgsConstructor
@Validated
@PreAuthorize("isAuthenticated()")
public class SessionController extends AbstractController {

    private final SessionService sessionService;

    // Admin only
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<SessionResponseDTO>> getAllSessions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endDate,
            Pageable pageable) {

        Page<Session> sessions = sessionService.getAllSessions(status, search, startDate, endDate, pageable);

        Page<SessionResponseDTO> response = sessions.map(this::toSessionResponseDTO);

        return sendOkResponse(response);
    }

    // Admin or Mentor
    @GetMapping("{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MENTOR')")
    public ResponseEntity<SessionResponseDTO> getSessionById(@PathVariable Long id) {

        Session session = sessionService.getSessionById(id);

        return sendOkResponse(toSessionResponseDTO(session));
    }

    // Admin creates sessions manually
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SessionResponseDTO> createSession(
            @Valid @RequestBody SessionRequestDTO sessionDTO) {

        Session session = sessionService.createNewSession(sessionDTO);

        return sendCreatedResponse(toSessionResponseDTO(session));
    }

    // Admin updates sessions
    @PutMapping("{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SessionResponseDTO> updateSession(
            @PathVariable Long id,
            @Valid @RequestBody SessionRequestDTO updatedSessionDTO) {

        Session session = sessionService.updateSessionById(id, updatedSessionDTO);

        return sendOkResponse(toSessionResponseDTO(session));
    }

    // Admin delete
    @DeleteMapping("{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSession(@PathVariable Long id) {

        sessionService.deleteSession(id);

        return sendNoContentResponse();
    }

    // Student enroll to a session
    @PostMapping("/enroll")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<SessionResponseDTO> enroll(
            @RequestBody SessionRequestDTO sessionDTO,
            Authentication authentication) {

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        Session session = sessionService.enrollSession(userPrincipal, sessionDTO);

        return sendCreatedResponse(toSessionResponseDTO(session));
    }

    // Logged student sessions
    @GetMapping("/my-sessions")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<SessionResponseDTO>> getMySessions(Authentication authentication) {

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        List<Session> sessions =
                sessionService.getSessionsByStudent(userPrincipal);

        List<SessionResponseDTO> response = sessions.stream()
                .map(this::toSessionResponseDTO)
                .collect(Collectors.toList());

        return sendOkResponse(response);
    }

    @PostMapping(path = "{id}/payment-proof", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<SessionResponseDTO> uploadPaymentProof(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam("paymentMethod") String paymentMethod,
            @RequestParam(value = "paymentReference", required = false) String paymentReference,
            @RequestParam(value = "paymentNotes", required = false) String paymentNotes,
            Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Session session = sessionService.uploadPaymentProof(id, userPrincipal, file, paymentMethod, paymentReference, paymentNotes);
        return sendOkResponse(toSessionResponseDTO(session));
    }

    @GetMapping("{id}/payment-proof")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> downloadPaymentProof(@PathVariable Long id, Authentication authentication) {
        Session session = sessionService.getSessionById(id);
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        boolean isOwner = session.getStudent() != null
                && session.getStudent().getStudentId() != null
                && session.getStudent().getStudentId().equals(userPrincipal.getId());

        if (!isAdmin && !isOwner) {
            throw new SkillMentorException("You are not allowed to view this payment proof", HttpStatus.FORBIDDEN);
        }
        if (session.getPaymentProofPath() == null || session.getPaymentProofPath().isBlank()) {
            throw new SkillMentorException("Payment proof not found", HttpStatus.NOT_FOUND);
        }

        try {
            Path filePath = Path.of(session.getPaymentProofPath());
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new SkillMentorException("Payment proof not found", HttpStatus.NOT_FOUND);
            }

            MediaType contentType = MediaTypeFactory.getMediaType(resource)
                    .orElse(MediaType.APPLICATION_OCTET_STREAM);

            return ResponseEntity.ok()
                    .contentType(contentType)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + (session.getPaymentProofFileName() != null
                                    ? session.getPaymentProofFileName()
                                    : filePath.getFileName()) + "\"")
                    .body(resource);
        } catch (MalformedURLException exception) {
            throw new SkillMentorException("Payment proof not found", HttpStatus.NOT_FOUND);
        }
    }

    // Admin: confirm payment
    @PatchMapping("{id}/confirm-payment")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SessionResponseDTO> confirmPayment(@PathVariable Long id) {
        Session session = sessionService.confirmPayment(id);
        return sendOkResponse(toSessionResponseDTO(session));
    }

    // Admin: mark complete
    @PatchMapping("{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SessionResponseDTO> markComplete(@PathVariable Long id) {
        Session session = sessionService.markComplete(id);
        return sendOkResponse(toSessionResponseDTO(session));
    }

    // Admin: add meeting link
    @PatchMapping("{id}/meeting-link")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SessionResponseDTO> addMeetingLink(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        Session session = sessionService.addMeetingLink(id, body.get("meetingLink"));
        return sendOkResponse(toSessionResponseDTO(session));
    }

    // Mapper
    private SessionResponseDTO toSessionResponseDTO(Session session) {

        SessionResponseDTO sessionResponseDTO = new SessionResponseDTO();

        sessionResponseDTO.setId(session.getId());

        if (session.getStudent() != null) {
            sessionResponseDTO.setStudentName(session.getStudent().getFirstName() + " " +
                    session.getStudent().getLastName());
        }

        if (session.getMentor() != null) {
            sessionResponseDTO.setMentorId(session.getMentor().getId());
            sessionResponseDTO.setMentorClerkUserId(session.getMentor().getMentorId());
            sessionResponseDTO.setMentorName(session.getMentor().getFirstName() + " " +
                    session.getMentor().getLastName());
            sessionResponseDTO.setMentorProfileImageUrl(session.getMentor().getProfileImageUrl());
        }

        if (session.getSubject() != null) {
            sessionResponseDTO.setSubjectId(session.getSubject().getId());
            sessionResponseDTO.setSubjectName(session.getSubject().getSubjectName());
        }

        sessionResponseDTO.setSessionAt(session.getSessionAt());
        sessionResponseDTO.setDurationMinutes(session.getDurationMinutes());
        sessionResponseDTO.setSessionStatus(session.getSessionStatus() != null ? session.getSessionStatus().name() : null);
        sessionResponseDTO.setPaymentStatus(session.getPaymentStatus() != null ? session.getPaymentStatus().name() : null);
        sessionResponseDTO.setPaymentMethod(session.getPaymentMethod());
        sessionResponseDTO.setPaymentReference(session.getPaymentReference());
        sessionResponseDTO.setPaymentNotes(session.getPaymentNotes());
        sessionResponseDTO.setPaymentProofFileName(session.getPaymentProofFileName());
        sessionResponseDTO.setHasPaymentProof(session.getPaymentProofPath() != null && !session.getPaymentProofPath().isBlank());
        sessionResponseDTO.setPaymentSubmittedAt(session.getPaymentSubmittedAt());
        sessionResponseDTO.setMeetingLink(session.getMeetingLink());
        sessionResponseDTO.setReviewSubmitted(session.getReview() != null);
        if (session.getReview() != null) {
            sessionResponseDTO.setReviewId(session.getReview().getId());
        }
        sessionResponseDTO.setCreatedAt(session.getCreatedAt());
        sessionResponseDTO.setUpdatedAt(session.getUpdatedAt());

        return sessionResponseDTO;
    }
}