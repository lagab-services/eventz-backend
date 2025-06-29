package com.lagab.eventz.app.domain.auth.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TokenValidationResponse(
        @JsonProperty("is_valid")
        Boolean isValid,
        @JsonProperty("expires_at")
        LocalDateTime expiresAt,
        UserResponse user
) {
}
