package com.stemlink.skillmentor.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.stemlink.skillmentor.services.ClerkUserService;
import com.stemlink.skillmentor.services.impl.dto.ClerkCreatedUserDetails;
import com.stemlink.skillmentor.services.impl.dto.ClerkUserDetails;
import com.stemlink.skillmentor.exceptions.SkillMentorException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.security.SecureRandom;

@Service
@Slf4j
public class ClerkUserServiceImpl implements ClerkUserService {

    private static final String CLERK_API_BASE = "https://api.clerk.com";
    private static final String UPPERCASE = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String SPECIALS = "!@#$%^&*";
    private static final String ALL_PASSWORD_CHARS = UPPERCASE + LOWERCASE + DIGITS + SPECIALS;

    private final String clerkSecretKey;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;
    private final SecureRandom secureRandom = new SecureRandom();

    public ClerkUserServiceImpl(@Value("${clerk.secret.key:}") String clerkSecretKey) {
        this.clerkSecretKey = clerkSecretKey;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public Optional<ClerkUserDetails> getUserById(String clerkUserId) {
        if (clerkSecretKey == null || clerkSecretKey.isBlank()) {
            log.warn("Clerk secret key is not configured; cannot fetch user {}", clerkUserId);
            return Optional.empty();
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CLERK_API_BASE + "/v1/users/" + clerkUserId))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + clerkSecretKey)
                    .header(HttpHeaders.ACCEPT, "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == HttpStatus.NOT_FOUND.value()) {
                log.warn("Clerk user not found for id {}", clerkUserId);
                return Optional.empty();
            }

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("Failed to fetch Clerk user {}. Status: {}, Body: {}", clerkUserId, response.statusCode(), response.body());
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(response.body());
            String email = extractPrimaryEmail(root);
            String firstName = textOrNull(root.get("first_name"));
            String lastName = textOrNull(root.get("last_name"));
            String role = extractRole(root);

            return Optional.of(new ClerkUserDetails(email, firstName, lastName, role));
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Error fetching Clerk user {}", clerkUserId, e);
            return Optional.empty();
        }
    }

    @Override
    public boolean updateUserPublicRole(String clerkUserId, String role) {
        if (clerkSecretKey == null || clerkSecretKey.isBlank()) {
            log.warn("Clerk secret key is not configured; cannot update metadata for user {}", clerkUserId);
            return false;
        }

        try {
            ObjectNode body = objectMapper.createObjectNode();
            ObjectNode publicMetadata = body.putObject("public_metadata");
            publicMetadata.put("role", role);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CLERK_API_BASE + "/v1/users/" + clerkUserId + "/metadata"))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + clerkSecretKey)
                    .header(HttpHeaders.ACCEPT, "application/json")
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("Failed to update Clerk metadata for user {}. Status: {}, Body: {}", clerkUserId, response.statusCode(), response.body());
                return false;
            }

            log.info("Updated Clerk public metadata role={} for user {}", role, clerkUserId);
            return true;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Error updating Clerk metadata for user {}", clerkUserId, e);
            return false;
        }
    }

    @Override
    public ClerkCreatedUserDetails createMentorUser(String email, String firstName, String lastName) {
        if (clerkSecretKey == null || clerkSecretKey.isBlank()) {
            throw new SkillMentorException("Clerk secret key is not configured", HttpStatus.BAD_GATEWAY);
        }

        String temporaryPassword = generateTemporaryPassword();

        try {
            ObjectNode body = objectMapper.createObjectNode();
            ArrayNode emailAddresses = body.putArray("email_address");
            emailAddresses.add(email);
            body.put("first_name", firstName);
            body.put("last_name", lastName);
            body.put("password", temporaryPassword);

            ObjectNode publicMetadata = body.putObject("public_metadata");
            publicMetadata.put("role", "mentor");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CLERK_API_BASE + "/v1/users"))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + clerkSecretKey)
                    .header(HttpHeaders.ACCEPT, "application/json")
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("Failed to create Clerk mentor user. Status: {}, Body: {}", response.statusCode(), response.body());
                throw new SkillMentorException(extractErrorMessage(response.body(), "Failed to create mentor login in Clerk"), HttpStatus.CONFLICT);
            }

            JsonNode root = objectMapper.readTree(response.body());
            return new ClerkCreatedUserDetails(
                    textOrNull(root.get("id")),
                    email,
                    temporaryPassword
            );
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Error creating Clerk mentor user for {}", email, e);
            throw new SkillMentorException("Failed to create mentor login in Clerk", HttpStatus.BAD_GATEWAY);
        }
    }

    @Override
    public boolean deleteUser(String clerkUserId) {
        if (clerkSecretKey == null || clerkSecretKey.isBlank()) {
            return false;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CLERK_API_BASE + "/v1/users/" + clerkUserId))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + clerkSecretKey)
                    .header(HttpHeaders.ACCEPT, "application/json")
                    .DELETE()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Error deleting Clerk user {}", clerkUserId, e);
            return false;
        }
    }

    private String extractPrimaryEmail(JsonNode root) {
        String primaryEmailId = textOrNull(root.get("primary_email_address_id"));
        JsonNode emailAddresses = root.get("email_addresses");
        if (emailAddresses == null || !emailAddresses.isArray()) {
            return null;
        }

        String fallbackEmail = null;
        for (JsonNode emailNode : emailAddresses) {
            String email = textOrNull(emailNode.get("email_address"));
            if (fallbackEmail == null) {
                fallbackEmail = email;
            }

            String emailId = textOrNull(emailNode.get("id"));
            if (primaryEmailId != null && primaryEmailId.equals(emailId)) {
                return email;
            }
        }

        return fallbackEmail;
    }

    private String textOrNull(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }

    private String extractRole(JsonNode root) {
        JsonNode publicMetadata = root.get("public_metadata");
        if (publicMetadata == null || publicMetadata.isNull()) {
            return null;
        }

        JsonNode roleNode = publicMetadata.get("role");
        return textOrNull(roleNode);
    }

    private String generateTemporaryPassword() {
        StringBuilder builder = new StringBuilder(14);
        builder.append(randomChar(UPPERCASE));
        builder.append(randomChar(LOWERCASE));
        builder.append(randomChar(DIGITS));
        builder.append(randomChar(SPECIALS));

        for (int i = 4; i < 14; i++) {
            builder.append(randomChar(ALL_PASSWORD_CHARS));
        }

        return shuffle(builder.toString());
    }

    private char randomChar(String chars) {
        return chars.charAt(secureRandom.nextInt(chars.length()));
    }

    private String shuffle(String input) {
        char[] chars = input.toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int index = secureRandom.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[index];
            chars[index] = temp;
        }
        return new String(chars);
    }

    private String extractErrorMessage(String responseBody, String fallback) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode errors = root.get("errors");
            if (errors != null && errors.isArray() && !errors.isEmpty()) {
                JsonNode firstError = errors.get(0);
                String message = textOrNull(firstError.get("message"));
                if (message != null && !message.isBlank()) {
                    return message;
                }
                String longMessage = textOrNull(firstError.get("long_message"));
                if (longMessage != null && !longMessage.isBlank()) {
                    return longMessage;
                }
            }
        } catch (Exception ignored) {
            // Fall back to the default message if response parsing fails.
        }

        return fallback;
    }
}
