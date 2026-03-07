package com.webapp.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Main Application Entry Point
 * 
 * This is the main class that bootstraps the Spring Boot application.
 * It enables JPA auditing for automatic timestamp management.
 * Excludes UserDetailsServiceAutoConfiguration to prevent conflicts with JWT auth.
 */
@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
@EnableJpaAuditing
public class PersonalWebBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(PersonalWebBackendApplication.class, args);
        System.out.println("🚀 Personal Web Backend Started Successfully!");
        System.out.println("📚 API Documentation: http://localhost:8080/api/swagger-ui.html");
    }
}
