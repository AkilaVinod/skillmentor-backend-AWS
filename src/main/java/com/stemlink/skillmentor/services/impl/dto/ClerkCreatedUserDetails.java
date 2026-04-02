package com.stemlink.skillmentor.services.impl.dto;

public record ClerkCreatedUserDetails(
        String userId,
        String email,
        String temporaryPassword
) {
}
