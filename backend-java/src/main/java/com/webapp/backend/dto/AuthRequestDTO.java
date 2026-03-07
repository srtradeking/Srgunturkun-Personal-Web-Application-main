package com.webapp.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for authentication requests
 * Contains credentials for user login (email + password)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequestDTO {
    
    @NotBlank(message = "Email cannot be blank")
    private String email;
    
    @NotBlank(message = "Password cannot be blank")
    private String password;
}
