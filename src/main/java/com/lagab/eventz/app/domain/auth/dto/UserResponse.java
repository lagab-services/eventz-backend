package com.lagab.eventz.app.domain.auth.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        String role,
        String locale,
        @JsonProperty("is_active")
        Boolean isActive,
        @JsonProperty("is_email_verified")
        Boolean isEmailVerified,
        @JsonProperty("created_at")
        LocalDateTime createdAt
) {
}
