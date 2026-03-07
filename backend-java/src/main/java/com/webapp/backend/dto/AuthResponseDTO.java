package com.webapp.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for authentication responses
 * Contains JWT tokens and user information after successful authentication
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {
    
    private String accessToken;
    
    private String refreshToken;
    
    private String username;

    private String email;

    private String displayName;

    private String role;
    
    private Long userId;
}
