package com.stemlink.skillmentor.controllers;

import com.stemlink.skillmentor.dto.request.ReviewRequestDTO;
import com.stemlink.skillmentor.dto.response.ReviewResponseDTO;
import jakarta.validation.Valid;
import com.stemlink.skillmentor.security.UserPrincipal;
import com.stemlink.skillmentor.services.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController extends AbstractController {

    private final ReviewService reviewService;

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ReviewResponseDTO> createReview(
            @Valid @RequestBody ReviewRequestDTO request,
            Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        ReviewResponseDTO dto = reviewService.createReview(request, userPrincipal.getId());
        return sendCreatedResponse(dto);
    }

    @PutMapping("{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ReviewResponseDTO> updateReview(
            @PathVariable Long id,
            @Valid @RequestBody ReviewRequestDTO request,
            Authentication authentication
    ) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        ReviewResponseDTO dto = reviewService.updateReview(id, request, userPrincipal.getId());
        return sendOkResponse(dto);
    }

    @DeleteMapping("{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long id,
            Authentication authentication
    ) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        reviewService.deleteReview(id, userPrincipal.getId());
        return sendNoContentResponse();
    }

    @GetMapping("/mentor/{mentorId}")
    public ResponseEntity<List<ReviewResponseDTO>> getMentorReviews(@PathVariable Long mentorId) {
        return sendOkResponse(reviewService.getReviewsByMentor(mentorId));
    }
}
