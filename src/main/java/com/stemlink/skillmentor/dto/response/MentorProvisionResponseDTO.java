package com.stemlink.skillmentor.dto.response;

import lombok.Data;

@Data
public class MentorProvisionResponseDTO {
    private MentorResponseDTO mentor;
    private String clerkUserId;
    private String loginEmail;
    private String temporaryPassword;
}
