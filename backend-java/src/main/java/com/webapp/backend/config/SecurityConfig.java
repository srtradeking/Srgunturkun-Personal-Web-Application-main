package com.webapp.backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Security Configuration
 * 
 * Configures Spring Security with JWT-based authentication,
 * stateless session management, and CORS integration.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ProfileAuthorizationFilter profileAuthorizationFilter;

    @Autowired
    private RateLimitingFilter rateLimitingFilter;

    @Autowired
    private com.webapp.backend.security.CommandInjectionFilter commandInjectionFilter;

    @Autowired
    private com.webapp.backend.security.CsrfProtectionFilter csrfProtectionFilter;

    @Autowired
    private com.webapp.backend.security.FileInclusionFilter fileInclusionFilter;

    @Autowired
    private com.webapp.backend.security.FileUploadSecurityFilter fileUploadSecurityFilter;

    @Autowired
    private com.webapp.backend.security.SQLInjectionFilter sqlInjectionFilter;

    @Autowired
    private com.webapp.backend.security.XSSFilter xssFilter;

    @Autowired
    private com.webapp.backend.security.JavaScriptSecurityFilter javaScriptSecurityFilter;

    @Autowired
    private com.webapp.backend.security.AuthorizationBypassFilter authorizationBypassFilter;

    @Autowired
    private com.webapp.backend.security.OpenRedirectFilter openRedirectFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            // Enable CSRF protection with custom configuration
            .csrf(csrf -> csrf
                .ignoringRequestMatchers(
                    // Public endpoints that don't need CSRF protection
                    "/api/auth/login",
                    "/api/auth/register",
                    "/api/auth/refresh",
                    "/api/auth/verify-email",
                    "/api/auth/resend-verification",
                    "/auth/login",
                    "/auth/register",
                    "/auth/refresh",
                    "/auth/verify-email",
                    "/auth/resend-verification",
                    // Read-only public endpoints
                    "/api/health/**",
                    "/api/public/**",
                    "/api/posts",
                    "/api/posts/**",
                    "/api/comments",
                    "/api/comments/**",
                    "/api/profiles",
                    "/api/profiles/**",
                    "/api/reports/**",
                    "/api/storage/**",
                    "/api/images/**",
                    "/api/videos/**",
                    "/health/**",
                    "/public/**",
                    "/posts",
                    "/posts/**",
                    "/comments",
                    "/comments/**",
                    "/profiles",
                    "/profiles/**",
                    "/reports/**",
                    "/storage/**",
                    "/images/**",
                    "/videos/**",
                    // Development/monitoring endpoints
                    "/api/swagger-ui/**",
                    "/api/v3/api-docs/**",
                    "/api/h2-console/**",
                    "/api/actuator/**",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/h2-console/**",
                    "/actuator/**"
                )
            )
            .formLogin(form -> form.disable())  // Explicitly disable form login
            .httpBasic(basic -> basic.disable())  // Explicitly disable HTTP basic auth
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Allow all OPTIONS requests (preflight) regardless of path
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(
                        "/api/auth/**",
                        "/api/health",
                        "/api/health/**",
                        "/api/swagger-ui/**",
                        "/api/swagger-ui.html",
                        "/api/v3/api-docs/**",
                        "/api/api-docs/**",
                        "/api/swagger-resources/**",
                        "/api/webjars/**",
                        "/api/h2-console/**",
                        "/api/actuator/**",
                        "/api/games/active",
                        "/api/public/**",
                        "/api/ip-address",
                        "/api/posts",
                        "/api/posts/**",
                        "/api/comments",
                        "/api/comments/**",
                        "/api/profiles",
                        "/api/profiles/**",
                        "/api/reports/**",
                        // Non-prefixed paths (for direct routing)
                        "/auth/**",
                        "/health/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/api-docs/**",
                        "/swagger-resources/**",
                        "/webjars/**",
                        "/h2-console/**",
                        "/actuator/**",
                        "/games/active",
                        "/public/**",
                        "/ip-address",
                        "/posts",
                        "/posts/**",
                        "/comments",
                        "/comments/**",
                        "/profiles",
                        "/profiles/**",
                        "/reports/**"
                ).permitAll()
                .requestMatchers(HttpMethod.POST, "/api/consent-audits", "/consent-audits").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/consent-audits/by-ip/**", "/consent-audits/by-ip/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/storage/**", "/storage/**").permitAll()
                .anyRequest().authenticated()
            )
            // Register JavaScript security filter first
            .addFilterBefore(javaScriptSecurityFilter, UsernamePasswordAuthenticationFilter.class)
            // Register XSS filter
            .addFilterAfter(xssFilter, com.webapp.backend.security.JavaScriptSecurityFilter.class)
            // Register SQL injection filter
            .addFilterAfter(sqlInjectionFilter, com.webapp.backend.security.XSSFilter.class)
            // Register file upload security filter
            .addFilterAfter(fileUploadSecurityFilter, com.webapp.backend.security.SQLInjectionFilter.class)
            // Register file inclusion filter to block LFI/RFI attempts
            .addFilterAfter(fileInclusionFilter, com.webapp.backend.security.FileUploadSecurityFilter.class)
            // Register command injection filter to validate all inputs
            .addFilterAfter(commandInjectionFilter, com.webapp.backend.security.FileInclusionFilter.class)
            // Register rate limiting filter to protect against brute force
            .addFilterAfter(rateLimitingFilter, com.webapp.backend.security.CommandInjectionFilter.class)
            // Register JWT filter
            .addFilterAfter(jwtAuthenticationFilter, RateLimitingFilter.class)
            // Register CSRF protection filter after JWT authentication
            .addFilterAfter(csrfProtectionFilter, com.webapp.backend.config.JwtAuthenticationFilter.class)
            // Register authorization bypass filter
            .addFilterAfter(authorizationBypassFilter, com.webapp.backend.security.CsrfProtectionFilter.class)
            // Register open redirect filter
            .addFilterAfter(openRedirectFilter, com.webapp.backend.security.AuthorizationBypassFilter.class)
            // Profile authorization filter for user-specific endpoints
            .addFilterAfter(profileAuthorizationFilter, com.webapp.backend.security.OpenRedirectFilter.class)
            // Allow H2 console to be embedded in iframe
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
