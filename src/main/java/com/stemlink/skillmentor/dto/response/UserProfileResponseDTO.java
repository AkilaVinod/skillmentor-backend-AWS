package com.stemlink.skillmentor.dto.response;

import lombok.Data;

import java.util.Date;

@Data
public class UserProfileResponseDTO {
    private Long id;
    private String clerkUserId;
    private String email;
    private String firstName;
    private String lastName;
    private String role;        // "STUDENT" | "MENTOR" | "ADMIN"
    private boolean isNewUser;  // kept for frontend compatibility, now always false
    private Date createdAt;
}
