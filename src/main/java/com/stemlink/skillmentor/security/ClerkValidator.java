package com.stemlink.skillmentor.security;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.net.URL;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


@Slf4j
public class ClerkValidator implements TokenValidator {

    private final JwkProvider jwkProvider;

    public ClerkValidator(@Value("${clerk.jwks.url}") String clerkJwksUrl) {
        try {
            this.jwkProvider = new UrlJwkProvider(new URL(clerkJwksUrl));
        } catch (Exception e) {
            log.error("Failed to initialize JwkProvider with URL: {}", clerkJwksUrl, e);
            throw new RuntimeException("Failed to initialize Clerk validator", e);
        }
    }

    @Override
    public boolean validateToken(String token){
        try {
            // Step 1: Decode JWT without verification to get header info
            DecodedJWT decodedJWT = decodeToken(token);
            if (decodedJWT == null) {
                log.error("Failed to decode token");
                return false;
            }

            // Step 2: Extract key ID (kid) from the token header
            String kid = decodedJWT.getKeyId();
            if (kid == null || kid.isEmpty()) {
                log.error("Token does not contain a key ID (kid)");
                return false;
            }

            log.debug("Token kid: {}", kid);

            // Step 3: Fetch JWK and verify signature
            if (!verifyTokenSignature(token, kid)) {
                log.error("Token signature verification failed");
                return false;
            }

            log.debug("Token validation successful for subject: {}", decodedJWT.getSubject());
            return true;

        } catch (Exception e) {
            log.error("Error validating token: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String extractUserId(String token) {
        try {
            if (!validateToken(token)) {
                return null;
            }
            DecodedJWT decodedJWT = decodeToken(token);
            return decodedJWT != null ? decodedJWT.getSubject() : null;
        } catch (Exception e) {
            log.error("Error extracting user ID: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public List<String> extractRoles(String token) {
        try {
            if (!validateToken(token)) {
                return null;
            }
            DecodedJWT decodedJWT = decodeToken(token);
            if (decodedJWT == null) {
                return null;
            }
            Set<String> roles = new LinkedHashSet<>();

            addRoles(roles, decodedJWT.getClaim("roles").asList(String.class));

            extractRoleFromMap(decodedJWT.getClaim("public_metadata"))
                    .ifPresent(role -> roles.add(role.toUpperCase()));

            extractRoleFromMap(decodedJWT.getClaim("publicMetadata"))
                    .ifPresent(role -> roles.add(role.toUpperCase()));

            Claim directRoleClaim = decodedJWT.getClaim("role");
            if (!directRoleClaim.isNull()) {
                String directRole = directRoleClaim.asString();
                if (directRole != null && !directRole.isBlank()) {
                    roles.add(directRole.toUpperCase());
                }
            }

            return new ArrayList<>(roles);
        } catch (Exception e) {
            log.error("Error extracting roles: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public String extractFirstName(String token) {
        try {
            DecodedJWT decodedJWT = decodeToken(token);
            return decodedJWT != null ? decodedJWT.getClaim("first_name").asString() : null;
        } catch (Exception e) {
            log.error("Error extracting first name: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public String extractLastName(String token) {
        try {
            DecodedJWT decodedJWT = decodeToken(token);
            return decodedJWT != null ? decodedJWT.getClaim("last_name").asString() : null;
        } catch (Exception e) {
            log.error("Error extracting last name: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public String extractEmail(String token) {
        try {
            DecodedJWT decodedJWT = decodeToken(token);
            return decodedJWT != null ? decodedJWT.getClaim("email").asString() : null;
        } catch (Exception e) {
            log.error("Error extracting email: {}", e.getMessage());
            return null;
        }
    }


    private DecodedJWT decodeToken(String token) {
        try {
            return JWT.decode(token);
        } catch (Exception e) {
            log.error("Failed to decode token: {}", e.getMessage());
            return null;
        }
    }

    private boolean verifyTokenSignature(String token, String kid) {
        try {
            // Fetch the JWK from Clerk
            Jwk jwk = jwkProvider.get(kid);

            // Get the public key from the JWK
            PublicKey publicKey = jwk.getPublicKey();

            // Create algorithm and verify the token
            Algorithm algorithm = Algorithm.RSA256((java.security.interfaces.RSAPublicKey) publicKey, null);
            JWT.require(algorithm).acceptLeeway(60).build().verify(token);

            log.debug("Token signature verified successfully for kid: {}", kid);
            return true;

        } catch (Exception e) {
            log.error("Signature verification failed for kid {}: {}", kid, e.getMessage());
            return false;
        }
    }

    private void addRoles(Set<String> roles, List<String> roleClaims) {
        if (roleClaims == null) {
            return;
        }

        for (String role : roleClaims) {
            if (role != null && !role.isBlank()) {
                roles.add(role.toUpperCase());
            }
        }
    }

    private java.util.Optional<String> extractRoleFromMap(Claim claim) {
        if (claim == null || claim.isNull()) {
            return java.util.Optional.empty();
        }

        Map<String, Object> metadata = claim.asMap();
        if (metadata == null) {
            return java.util.Optional.empty();
        }

        Object role = metadata.get("role");
        if (role instanceof String roleString && !roleString.isBlank()) {
            return java.util.Optional.of(roleString);
        }

        return java.util.Optional.empty();
    }

}

