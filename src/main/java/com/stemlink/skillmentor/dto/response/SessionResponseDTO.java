package com.stemlink.skillmentor.dto.response;

import lombok.Data;

import java.util.Date;

@Data
public class SessionResponseDTO {
    private Long id;

    private Integer studentId;
    private String studentName;

    private Long mentorId;
    private String mentorClerkUserId;
    private String mentorName;
    private String mentorProfileImageUrl;

    private Long subjectId;
    private String subjectName;

    private Date sessionAt;
    private Integer durationMinutes;

    private String sessionStatus;
    private String meetingLink;

    private String sessionNotes;
    private String studentReview;
    private Integer studentRating;

    private String paymentStatus;
    private String paymentMethod;
    private String paymentReference;
    private String paymentNotes;
    private String paymentProofFileName;
    private boolean hasPaymentProof;
    private Date paymentSubmittedAt;
    private Long reviewId;
    private boolean reviewSubmitted;

    private Date createdAt;
    private Date updatedAt;

}
