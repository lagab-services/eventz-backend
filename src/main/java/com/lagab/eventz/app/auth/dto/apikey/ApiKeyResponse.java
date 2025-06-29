package com.lagab.eventz.app.auth.dto.apikey;

import java.time.LocalDateTime;
import java.util.Set;

import com.lagab.eventz.app.auth.entity.ApiKey;
import com.lagab.eventz.app.user.entity.Role;

public record ApiKeyResponse(
        Long id,
        String name,
        String clientType,
        //String clientId,
        String clientSecret, // Only during creation
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        boolean active,
        Set<Role> roles
) {
    // Factory methods to control keyValue exposure
    public static ApiKeyResponse withKey(ApiKey apiKey) {
        return new ApiKeyResponse(
                apiKey.getId(),
                apiKey.getName(),
                apiKey.getClientType(),
                //apiKey.getClientId(),
                apiKey.getClientSecret(),
                apiKey.getCreatedAt(),
                apiKey.getExpiresAt(),
                apiKey.isActive(),
                apiKey.getRoles()
        );
    }

    public static ApiKeyResponse withoutKey(ApiKey apiKey) {
        return new ApiKeyResponse(
                apiKey.getId(),
                apiKey.getName(),
                apiKey.getClientType(),
                //apiKey.getClientId(),
                null, // Hides the key for security reasons
                apiKey.getCreatedAt(),
                apiKey.getExpiresAt(),
                apiKey.isActive(),
                apiKey.getRoles()
        );
    }
}
