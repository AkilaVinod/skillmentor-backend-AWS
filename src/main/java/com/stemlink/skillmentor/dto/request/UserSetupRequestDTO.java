package com.stemlink.skillmentor.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserSetupRequestDTO {
    @NotBlank
    private String role; // "STUDENT" or "MENTOR"
}
