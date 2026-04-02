package com.stemlink.skillmentor.services.impl;

import com.stemlink.skillmentor.Repositories.MentorAvailabilityRepository;
import com.stemlink.skillmentor.Repositories.MentorRepository;
import com.stemlink.skillmentor.Repositories.SessionRepository;
import com.stemlink.skillmentor.Repositories.StudentRepository;
import com.stemlink.skillmentor.Repositories.SubjectRepository;
import com.stemlink.skillmentor.Repositories.UserProfileRepository;
import com.stemlink.skillmentor.constants.PaymentStatus;
import com.stemlink.skillmentor.constants.SessionStatus;
import com.stemlink.skillmentor.dto.request.SessionRequestDTO;
import com.stemlink.skillmentor.entities.Mentor;
import com.stemlink.skillmentor.entities.MentorAvailability;
import com.stemlink.skillmentor.entities.Session;
import com.stemlink.skillmentor.entities.Student;
import com.stemlink.skillmentor.entities.Subject;
import com.stemlink.skillmentor.entities.UserProfile;
import com.stemlink.skillmentor.exceptions.SkillMentorException;
import com.stemlink.skillmentor.security.UserPrincipal;
import com.stemlink.skillmentor.services.SessionService;
import com.stemlink.skillmentor.utils.ValidationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private static final List<SessionStatus> ACTIVE_SESSION_STATUSES = List.of(
            SessionStatus.PENDING,
            SessionStatus.CONFIRMED
    );

    private final SessionRepository sessionRepository;
    private final StudentRepository studentRepository;
    private final MentorRepository mentorRepository;
    private final SubjectRepository subjectRepository;
    private final MentorAvailabilityRepository mentorAvailabilityRepository;
    private final UserProfileRepository userProfileRepository;
    @Value("${app.payment.upload-dir:uploads/payment-proofs}")
    private String paymentUploadDir;

    @Override
    @CacheEvict(value = {"sessions", "session", "studentSessions"}, allEntries = true)
    public Session createNewSession(SessionRequestDTO sessionRequestDTO) {
        Student student = studentRepository.findById(sessionRequestDTO.getStudentId()).orElseThrow(
                () -> new SkillMentorException("Student not found", HttpStatus.NOT_FOUND)
        );
        Mentor mentor = getMentorByClerkId(sessionRequestDTO.getMentorId());
        Subject subject = getSubjectById(sessionRequestDTO.getSubjectId());

        validateSessionRequest(student, mentor, subject, sessionRequestDTO.getSessionAt(),
                sessionRequestDTO.getDurationMinutes(), null);

        Session session = new Session();
        session.setStudent(student);
        session.setMentor(mentor);
        session.setSubject(subject);
        session.setSessionAt(sessionRequestDTO.getSessionAt());
        session.setDurationMinutes(ValidationUtils.normalizeDuration(sessionRequestDTO.getDurationMinutes()));
        session.setSessionNotes(sessionRequestDTO.getSessionNotes());
        session.setMeetingLink(sessionRequestDTO.getMeetingLink());
        session.setSessionStatus(parseSessionStatus(sessionRequestDTO.getSessionStatus(), SessionStatus.PENDING));
        session.setPaymentStatus(parsePaymentStatus(sessionRequestDTO.getPaymentStatus(), PaymentStatus.PENDING));

        return sessionRepository.save(session);
    }

    @Override
    @Cacheable(value = "sessions", key = "#status + '-' + #search + '-' + #startDate + '-' + #endDate + '-' + #pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort")
    public Page<Session> getAllSessions(String status, String search, Date startDate, Date endDate, Pageable pageable) {
        log.info("Fetching filtered sessions page {} from DB", pageable.getPageNumber());
        SessionStatus statusEnum = parseSessionStatus(status, null);
        String searchNorm = normalizeSearch(search);
        Specification<Session> spec = (root, query, cb) -> {
            query.distinct(true);
            var predicates = new java.util.ArrayList<Predicate>();
            if (statusEnum != null) {
                predicates.add(cb.equal(root.get("sessionStatus"), statusEnum));
            }
            if (searchNorm != null && !searchNorm.isBlank()) {
                var student = root.join("student", JoinType.LEFT);
                var mentor = root.join("mentor", JoinType.LEFT);
                String pattern = "%" + searchNorm.toLowerCase() + "%";
                Predicate studentMatch = cb.like(cb.lower(cb.concat(cb.coalesce(student.get("firstName"), ""), cb.concat(" ", cb.coalesce(student.get("lastName"), "")))), pattern);
                Predicate mentorMatch = cb.like(cb.lower(cb.concat(cb.coalesce(mentor.get("firstName"), ""), cb.concat(" ", cb.coalesce(mentor.get("lastName"), "")))), pattern);
                predicates.add(cb.or(studentMatch, mentorMatch));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("sessionAt"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("sessionAt"), endDate));
            }
            return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
        };
        return sessionRepository.findAll(spec, pageable);
    }

    @Override
    @Cacheable(value = "session", key = "#id")
    public Session getSessionById(Long id) {
        log.info("Fetching session {} from DB", id);
        return sessionRepository.findById(id).orElseThrow(
                () -> new SkillMentorException("Session not found", HttpStatus.NOT_FOUND)
        );
    }

    @Override
    @CacheEvict(value = {"sessions", "session", "studentSessions"}, allEntries = true)
    public Session updateSessionById(Long id, SessionRequestDTO updatedSessionDTO) {
        Session session = sessionRepository.findById(id).orElseThrow(
                () -> new SkillMentorException("Session not found", HttpStatus.NOT_FOUND)
        );

        Student student = updatedSessionDTO.getStudentId() != null
                ? studentRepository.findById(updatedSessionDTO.getStudentId()).orElseThrow(
                () -> new SkillMentorException("Student not found", HttpStatus.NOT_FOUND)
        )
                : session.getStudent();

        Mentor mentor = updatedSessionDTO.getMentorId() != null
                ? getMentorByClerkId(updatedSessionDTO.getMentorId())
                : session.getMentor();

        Subject subject = updatedSessionDTO.getSubjectId() != null
                ? getSubjectById(updatedSessionDTO.getSubjectId())
                : session.getSubject();

        Date sessionAt = updatedSessionDTO.getSessionAt() != null ? updatedSessionDTO.getSessionAt() : session.getSessionAt();
        Integer duration = updatedSessionDTO.getDurationMinutes() != null
                ? updatedSessionDTO.getDurationMinutes()
                : session.getDurationMinutes();

        validateSessionRequest(student, mentor, subject, sessionAt, duration, session.getId());

        session.setStudent(student);
        session.setMentor(mentor);
        session.setSubject(subject);
        session.setSessionAt(sessionAt);
        session.setDurationMinutes(ValidationUtils.normalizeDuration(duration));

        if (updatedSessionDTO.getSessionStatus() != null) {
            session.setSessionStatus(parseSessionStatus(updatedSessionDTO.getSessionStatus(), session.getSessionStatus()));
        }
        if (updatedSessionDTO.getPaymentStatus() != null) {
            session.setPaymentStatus(parsePaymentStatus(updatedSessionDTO.getPaymentStatus(), session.getPaymentStatus()));
        }
        if (updatedSessionDTO.getMeetingLink() != null) {
            session.setMeetingLink(updatedSessionDTO.getMeetingLink());
        }
        if (updatedSessionDTO.getSessionNotes() != null) {
            session.setSessionNotes(updatedSessionDTO.getSessionNotes());
        }

        return sessionRepository.save(session);
    }

    @Override
    @CacheEvict(value = {"sessions", "session", "studentSessions"}, allEntries = true)
    public void deleteSession(Long id) {
        Session session = sessionRepository.findById(id).orElseThrow(
                () -> new SkillMentorException("Session not found", HttpStatus.NOT_FOUND)
        );
        sessionRepository.delete(session);
    }

    @Override
    @CacheEvict(value = {"sessions", "session", "studentSessions"}, allEntries = true)
    public Session enrollSession(UserPrincipal userPrincipal, SessionRequestDTO sessionRequestDTO) {
        Student student = resolveStudent(userPrincipal);

        Mentor mentor = getMentorByClerkId(sessionRequestDTO.getMentorId());
        Subject subject = getSubjectById(sessionRequestDTO.getSubjectId());

        validateSessionRequest(student, mentor, subject, sessionRequestDTO.getSessionAt(),
                sessionRequestDTO.getDurationMinutes(), null);

        Session session = new Session();
        session.setStudent(student);
        session.setMentor(mentor);
        session.setSubject(subject);
        session.setSessionAt(sessionRequestDTO.getSessionAt());
        session.setDurationMinutes(ValidationUtils.normalizeDuration(sessionRequestDTO.getDurationMinutes()));
        session.setSessionStatus(SessionStatus.PENDING);
        session.setPaymentStatus(PaymentStatus.PENDING);

        return sessionRepository.save(session);
    }

    @Override
    @CacheEvict(value = {"sessions", "session", "studentSessions"}, allEntries = true)
    public Session confirmPayment(Long id) {
        Session session = sessionRepository.findById(id).orElseThrow(
                () -> new SkillMentorException("Session not found", HttpStatus.NOT_FOUND)
        );

        if (session.getPaymentStatus() != PaymentStatus.SUBMITTED) {
            throw new SkillMentorException("Payment must be submitted before it can be confirmed", HttpStatus.BAD_REQUEST);
        }
        if (session.getPaymentProofPath() == null || session.getPaymentProofPath().isBlank()) {
            throw new SkillMentorException("Payment proof is required before confirmation", HttpStatus.BAD_REQUEST);
        }

        session.setPaymentStatus(PaymentStatus.PAID);
        session.setSessionStatus(SessionStatus.CONFIRMED);
        return sessionRepository.save(session);
    }

    @Override
    @CacheEvict(value = {"sessions", "session", "studentSessions"}, allEntries = true)
    public Session markComplete(Long id) {
        Session session = sessionRepository.findById(id).orElseThrow(
                () -> new SkillMentorException("Session not found", HttpStatus.NOT_FOUND)
        );

        session.setSessionStatus(SessionStatus.COMPLETED);
        return sessionRepository.save(session);
    }

    @Override
    @CacheEvict(value = {"sessions", "session", "studentSessions"}, allEntries = true)
    public Session addMeetingLink(Long id, String meetingLink) {
        Session session = sessionRepository.findById(id).orElseThrow(
                () -> new SkillMentorException("Session not found", HttpStatus.NOT_FOUND)
        );
        session.setMeetingLink(meetingLink);
        return sessionRepository.save(session);
    }

    @Override
    @Cacheable(value = "studentSessions", key = "#email")
    public List<Session> getSessionsByStudent(UserPrincipal userPrincipal) {
        Student student = resolveStudent(userPrincipal);
        log.info("Fetching sessions for student {} from DB", student.getStudentId());
        return sessionRepository.findByStudent_StudentIdOrderBySessionAtDesc(student.getStudentId());
    }

    @Override
    @CacheEvict(value = {"sessions", "session", "studentSessions"}, allEntries = true)
    public Session uploadPaymentProof(Long id, UserPrincipal userPrincipal, MultipartFile file, String paymentMethod,
                                      String paymentReference, String paymentNotes) {
        Session session = sessionRepository.findById(id).orElseThrow(
                () -> new SkillMentorException("Session not found", HttpStatus.NOT_FOUND)
        );

        if (session.getStudent() == null || session.getStudent().getStudentId() == null
                || !session.getStudent().getStudentId().equals(userPrincipal.getId())) {
            throw new SkillMentorException("You can only upload payment proof for your own sessions", HttpStatus.FORBIDDEN);
        }
        if (session.getSessionStatus() != SessionStatus.PENDING) {
            throw new SkillMentorException("Payment proof can only be uploaded for pending sessions", HttpStatus.BAD_REQUEST);
        }
        if (file == null || file.isEmpty()) {
            throw new SkillMentorException("Payment proof file is required", HttpStatus.BAD_REQUEST);
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new SkillMentorException("Payment proof must be 10MB or smaller", HttpStatus.BAD_REQUEST);
        }

        String normalizedMethod = normalizeRequiredText(paymentMethod, "Payment method");
        String normalizedReference = normalizeOptionalText(paymentReference);
        String normalizedNotes = normalizeOptionalText(paymentNotes);
        String originalFilename = sanitizeFileName(file.getOriginalFilename());
        String extension = "";
        int extensionIndex = originalFilename.lastIndexOf('.');
        if (extensionIndex >= 0) {
            extension = originalFilename.substring(extensionIndex);
        }

        Path uploadPath = Paths.get(paymentUploadDir).toAbsolutePath().normalize();
        Path destinationPath = uploadPath.resolve(id + "-" + UUID.randomUUID() + extension).normalize();

        try {
            Files.createDirectories(uploadPath);
            Files.copy(file.getInputStream(), destinationPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new SkillMentorException("Failed to store payment proof", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        deleteStoredProofIfPresent(session);

        session.setPaymentMethod(normalizedMethod);
        session.setPaymentReference(normalizedReference);
        session.setPaymentNotes(normalizedNotes);
        session.setPaymentProofPath(destinationPath.toString());
        session.setPaymentProofFileName(originalFilename);
        session.setPaymentSubmittedAt(new Date());
        session.setPaymentStatus(PaymentStatus.SUBMITTED);

        return sessionRepository.save(session);
    }

    private Mentor getMentorByClerkId(String mentorId) {
        return mentorRepository.findByMentorId(mentorId).orElseThrow(
                () -> new SkillMentorException("Mentor not found with mentorId: " + mentorId, HttpStatus.NOT_FOUND)
        );
    }

    private Subject getSubjectById(Long subjectId) {
        return subjectRepository.findById(subjectId).orElseThrow(
                () -> new SkillMentorException("Subject not found", HttpStatus.NOT_FOUND)
        );
    }

    private Student resolveStudent(UserPrincipal userPrincipal) {
        if (userPrincipal.getId() == null || userPrincipal.getId().isBlank()) {
            throw new SkillMentorException("Authenticated user id is missing", HttpStatus.UNAUTHORIZED);
        }

        UserProfile profile = userProfileRepository.findByClerkUserId(userPrincipal.getId())
                .orElseThrow(() -> new SkillMentorException(
                        "User profile not found. Refresh the page and try again.",
                        HttpStatus.BAD_REQUEST
                ));

        Student student = studentRepository.findByStudentId(userPrincipal.getId())
                .or(() -> studentRepository.findByEmail(profile.getEmail()))
                .orElseGet(Student::new);

        student.setStudentId(userPrincipal.getId());
        student.setEmail(profile.getEmail());
        student.setFirstName(resolveDisplayValue(profile.getFirstName(), userPrincipal.getFirstName()));
        student.setLastName(resolveDisplayValue(profile.getLastName(), userPrincipal.getLastName()));

        return studentRepository.save(student);
    }

    private void validateSessionRequest(
            Student student,
            Mentor mentor,
            Subject subject,
            Date sessionAt,
            Integer durationMinutes,
            Long excludedSessionId
    ) {
        ValidationUtils.validateSessionTime(sessionAt);

        if (subject.getMentor() == null || !subject.getMentor().getId().equals(mentor.getId())) {
            throw new SkillMentorException("Selected subject does not belong to the selected mentor", HttpStatus.BAD_REQUEST);
        }

        List<MentorAvailability> mentorAvailabilities =
                mentorAvailabilityRepository.findByMentorIdOrderByDayOfWeekAscStartTimeAsc(mentor.getId());
        List<Session> mentorSessions = sessionRepository.findByMentorIdAndSessionStatusIn(mentor.getId(), ACTIVE_SESSION_STATUSES);
        List<Session> studentSessions = sessionRepository.findByStudentIdAndSessionStatusIn(student.getId(), ACTIVE_SESSION_STATUSES);

        ValidationUtils.validateMentorAvailability(
                mentor,
                mentorAvailabilities,
                mentorSessions,
                sessionAt,
                durationMinutes,
                excludedSessionId
        );
        ValidationUtils.validateStudentAvailability(
                student,
                studentSessions,
                subject.getId(),
                sessionAt,
                durationMinutes,
                excludedSessionId
        );
    }

    private SessionStatus parseSessionStatus(String rawStatus, SessionStatus fallback) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return fallback;
        }

        try {
            return SessionStatus.valueOf(rawStatus.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new SkillMentorException("Invalid session status: " + rawStatus, HttpStatus.BAD_REQUEST);
        }
    }

    private PaymentStatus parsePaymentStatus(String rawStatus, PaymentStatus fallback) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return fallback;
        }

        try {
            return PaymentStatus.valueOf(rawStatus.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new SkillMentorException("Invalid payment status: " + rawStatus, HttpStatus.BAD_REQUEST);
        }
    }

    private String normalizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }

        return search.trim();
    }

    private String normalizeRequiredText(String value, String fieldName) {
        String normalized = normalizeOptionalText(value);
        if (normalized.isBlank()) {
            throw new SkillMentorException(fieldName + " is required", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private String normalizeOptionalText(String value) {
        return value == null ? "" : value.trim();
    }

    private String resolveDisplayValue(String primary, String fallback) {
        String normalizedPrimary = normalizeOptionalText(primary);
        if (!normalizedPrimary.isBlank()) {
            return normalizedPrimary;
        }
        return normalizeOptionalText(fallback);
    }

    private String sanitizeFileName(String originalFilename) {
        String fileName = originalFilename == null || originalFilename.isBlank() ? "payment-proof" : originalFilename;
        return fileName.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private void deleteStoredProofIfPresent(Session session) {
        if (session.getPaymentProofPath() == null || session.getPaymentProofPath().isBlank()) {
            return;
        }

        try {
            Files.deleteIfExists(Path.of(session.getPaymentProofPath()));
        } catch (IOException exception) {
            log.warn("Failed to delete previous payment proof for session {}", session.getId(), exception);
        }
    }
}