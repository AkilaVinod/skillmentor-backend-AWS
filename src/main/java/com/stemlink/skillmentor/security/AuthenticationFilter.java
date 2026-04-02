package com.stemlink.skillmentor.security;

import com.stemlink.skillmentor.Repositories.UserProfileRepository;
import com.stemlink.skillmentor.services.ClerkUserService;
import com.stemlink.skillmentor.services.impl.dto.ClerkUserDetails;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class AuthenticationFilter extends OncePerRequestFilter {

    private final TokenValidator tokenValidator;

    // @Lazy breaks the circular dependency: SecurityConfig → AuthFilter → UserProfileRepo → ... → SecurityConfig
    @Lazy
    @Autowired
    private UserProfileRepository userProfileRepository;

    @Lazy
    @Autowired
    private ClerkUserService clerkUserService;

    public AuthenticationFilter(TokenValidator tokenValidator) {
        this.tokenValidator = tokenValidator;
    }

    @Override
    protected void doFilterInternal(@Nonnull HttpServletRequest request,
                                    @Nonnull HttpServletResponse response,
                                    @Nonnull FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractToken(request);

        if (token != null && tokenValidator.validateToken(token)) {
            String userId    = tokenValidator.extractUserId(token);
            String email     = tokenValidator.extractEmail(token);
            String firstName = tokenValidator.extractFirstName(token);
            String lastName  = tokenValidator.extractLastName(token);

            UserPrincipal userPrincipal = new UserPrincipal(userId, email, firstName, lastName);

            List<GrantedAuthority> authorities = new ArrayList<>();

            // Check JWT for ADMIN (set via Clerk public metadata → "roles": ["ADMIN"])
            List<String> jwtRoles = tokenValidator.extractRoles(token);
            boolean isAdminFromJwt = jwtRoles != null &&
                    jwtRoles.stream().anyMatch(r -> r.equalsIgnoreCase("ADMIN"));

            boolean isAdminFromClerk = clerkUserService.getUserById(userId)
                    .map(ClerkUserDetails::role)
                    .map(role -> role != null && role.equalsIgnoreCase("admin"))
                    .orElse(false);

            if (isAdminFromJwt || isAdminFromClerk) {
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            } else {
                // Load the user's role from our DB for all non-admin users
                userProfileRepository.findByClerkUserId(userId).ifPresent(profile ->
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + profile.getRole().name()))
                );
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userPrincipal, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
