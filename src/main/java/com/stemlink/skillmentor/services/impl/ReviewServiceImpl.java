package com.stemlink.skillmentor.services.impl;

import com.stemlink.skillmentor.Repositories.MentorRepository;
import com.stemlink.skillmentor.Repositories.ReviewRepository;
import com.stemlink.skillmentor.Repositories.SessionRepository;
import com.stemlink.skillmentor.Repositories.StudentRepository;
import com.stemlink.skillmentor.constants.SessionStatus;
import com.stemlink.skillmentor.dto.request.ReviewRequestDTO;
import com.stemlink.skillmentor.dto.response.ReviewResponseDTO;
import com.stemlink.skillmentor.entities.Mentor;
import com.stemlink.skillmentor.entities.Review;
import com.stemlink.skillmentor.entities.Session;
import com.stemlink.skillmentor.entities.Student;
import com.stemlink.skillmentor.exceptions.SkillMentorException;
import com.stemlink.skillmentor.services.ReviewService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final MentorRepository mentorRepository;
    private final StudentRepository studentRepository;
    private final SessionRepository sessionRepository;
    private final ModelMapper modelMapper;

    @Override
    public ReviewResponseDTO createReview(ReviewRequestDTO request, String studentId) {
        Session session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new SkillMentorException("Session not found", HttpStatus.NOT_FOUND));

        if (session.getStudent() == null || session.getStudent().getStudentId() == null ||
                !session.getStudent().getStudentId().equals(studentId)) {
            throw new SkillMentorException("You can only review your own completed sessions", HttpStatus.FORBIDDEN);
        }

        if (session.getSessionStatus() != SessionStatus.COMPLETED) {
            throw new SkillMentorException("You can only review completed sessions", HttpStatus.BAD_REQUEST);
        }

        reviewRepository.findBySessionId(session.getId()).ifPresent(existing -> {
            throw new SkillMentorException("A review has already been submitted for this session", HttpStatus.CONFLICT);
        });

        Mentor mentor = mentorRepository.findById(session.getMentor().getId())
                .orElseThrow(() -> new SkillMentorException("Mentor not found", HttpStatus.NOT_FOUND));

        Student student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new SkillMentorException("Student not found", HttpStatus.NOT_FOUND));

        Review review = new Review();
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setMentor(mentor);
        review.setStudent(student);
        review.setSession(session);
        review.setCreatedAt(LocalDateTime.now());

        Review saved = reviewRepository.save(review);

        ReviewResponseDTO dto = modelMapper.map(saved, ReviewResponseDTO.class);
        dto.setSessionId(saved.getSession().getId());
        dto.setMentorId(saved.getMentor().getId());
        if (saved.getStudent() != null) {
            dto.setStudentId(saved.getStudent().getId());
            dto.setStudentName(saved.getStudent().getFirstName() + " " + saved.getStudent().getLastName());
        }
        return dto;
    }

    @Override
    public ReviewResponseDTO updateReview(Long reviewId, ReviewRequestDTO request, String studentId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new SkillMentorException("Review not found", HttpStatus.NOT_FOUND));

        if (review.getStudent() == null || review.getStudent().getStudentId() == null ||
                !review.getStudent().getStudentId().equals(studentId)) {
            throw new SkillMentorException("You can only update your own reviews", HttpStatus.FORBIDDEN);
        }

        if (review.getSession() == null || review.getSession().getSessionStatus() != SessionStatus.COMPLETED) {
            throw new SkillMentorException("You can only update reviews for completed sessions", HttpStatus.BAD_REQUEST);
        }

        review.setRating(request.getRating());
        review.setComment(request.getComment());

        Review saved = reviewRepository.save(review);

        ReviewResponseDTO dto = modelMapper.map(saved, ReviewResponseDTO.class);
        if (saved.getSession() != null) {
            dto.setSessionId(saved.getSession().getId());
        }
        if (saved.getMentor() != null) {
            dto.setMentorId(saved.getMentor().getId());
        }
        if (saved.getStudent() != null) {
            dto.setStudentId(saved.getStudent().getId());
            dto.setStudentName(saved.getStudent().getFirstName() + " " + saved.getStudent().getLastName());
        }
        return dto;
    }

    @Override
    public void deleteReview(Long reviewId, String studentId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new SkillMentorException("Review not found", HttpStatus.NOT_FOUND));

        if (review.getStudent() == null || review.getStudent().getStudentId() == null ||
                !review.getStudent().getStudentId().equals(studentId)) {
            throw new SkillMentorException("You can only delete your own reviews", HttpStatus.FORBIDDEN);
        }

        reviewRepository.delete(review);
    }

    @Override
    public List<ReviewResponseDTO> getReviewsByMentor(Long mentorId) {

        Mentor mentor = mentorRepository.findById(mentorId)
                .orElseThrow(() -> new RuntimeException("Mentor not found"));

        List<Review> reviews = reviewRepository.findByMentorId(mentor.getId());

        return reviews.stream().map(review -> {
            ReviewResponseDTO dto = modelMapper.map(review, ReviewResponseDTO.class);
            if (review.getSession() != null) {
                dto.setSessionId(review.getSession().getId());
            }
            if (review.getMentor() != null) {
                dto.setMentorId(review.getMentor().getId());
            }
            if (review.getStudent() != null) {
                dto.setStudentId(review.getStudent().getId());
                dto.setStudentName(review.getStudent().getFirstName() + " " + review.getStudent().getLastName());
            }
            return dto;
        }).toList();
    }

}
