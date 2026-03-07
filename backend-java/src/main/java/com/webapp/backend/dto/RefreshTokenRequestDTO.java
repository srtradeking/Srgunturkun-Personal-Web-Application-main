package com.webapp.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for refresh token requests
 * Contains the refresh token to exchange for a new access token
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequestDTO {
    
    @NotBlank(message = "Refresh token cannot be blank")
    private String refreshToken;
}
