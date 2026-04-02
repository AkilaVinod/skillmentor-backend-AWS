package com.stemlink.skillmentor.dto.response;

import lombok.Data;

import java.util.Date;
import java.util.List;


@Data
public class MentorResponseDTO {

    private Long id;

    private String mentorId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;

    private String title;
    private String profession;
    private String company;
    private int experienceYears;

    private String bio;
    private String profileImageUrl;

    private Double averageRating;
    private Integer totalReviews;
    private Integer totalEnrollments;
    private Integer totalStudentsTaught;
    private Integer positiveReviewPercentage;
    private Boolean isCertified;
    private String startYear;

    private List<SubjectResponseDTO> subjects;
    private List<MentorAvailabilityResponseDTO> availabilities;

    private Date createdAt;
    private Date updatedAt;
}
