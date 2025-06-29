package com.lagab.eventz.app.auth.dto.apikey;

import java.time.LocalDateTime;
import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record CreateApiKeyRequest(
        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Client type is required")
        String clientType,

        @NotEmpty(message = "At least one role is required")
        Set<String> roles,

        LocalDateTime expiresAt
) {
}
