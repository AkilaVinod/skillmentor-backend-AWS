package com.stemlink.skillmentor.configs;

import com.stemlink.skillmentor.security.AuthenticationFilter;
import com.stemlink.skillmentor.security.SkillMentorAccessDeniedHandler;
import com.stemlink.skillmentor.security.SkillMentorAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // ← enables @PreAuthorize on controllers
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthenticationFilter clerkAuthenticationFilter;
    private final SkillMentorAuthenticationEntryPoint skillMentorAuthenticationEntryPoint;
    private final SkillMentorAccessDeniedHandler skillMentorAccessDeniedHandler; // ← NEW
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(skillMentorAuthenticationEntryPoint)  // 401 handler
                        .accessDeniedHandler(skillMentorAccessDeniedHandler)            // 403 handler
                )
                .authorizeHttpRequests(auth -> auth

                        // ── Public routes ─────────────────────────────────────────
                        .requestMatchers(
                                "/api/public/**",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/webjars/**",
                                "/swagger-resources/**"
                        ).permitAll()

                        // ── Public read: Mentors ───────────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/api/v1/mentors", "/api/v1/mentors/**").permitAll()

                        // ── Public read: Reviews ───────────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/api/reviews/**").permitAll()

                        // ── Public read: Subjects ──────────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/api/v1/subjects/**").permitAll()

                        // ── User profile setup (any authenticated user, no role required) ──
                        .requestMatchers("/api/v1/users/me", "/api/v1/users/setup").authenticated()

                        // ── Role-based routes ─────────────────────────────────────
                        // Only ADMIN can manage users
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        // MENTOR or ADMIN can access mentor management
                        .requestMatchers(HttpMethod.POST, "/api/v1/mentors").hasAnyRole("ADMIN", "MENTOR")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/mentors/**").hasAnyRole("ADMIN", "MENTOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/mentors/**").hasRole("ADMIN")

                        // STUDENT or ADMIN can access student routes
                        .requestMatchers("/api/v1/students/**").hasAnyRole("ADMIN", "STUDENT")

                        // Everything else just needs to be authenticated
                        .anyRequest().authenticated()
                )
                .addFilterBefore(clerkAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
