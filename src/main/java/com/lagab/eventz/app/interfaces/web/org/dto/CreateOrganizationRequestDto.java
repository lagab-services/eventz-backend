package com.lagab.eventz.app.interfaces.web.org.dto;

import java.util.Map;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateOrganizationRequestDto(
        @NotBlank(message = "Organization name is required")
        @Size(min = 2, max = 100, message = "Organization name must be between 2 and 100 characters")
        String name,

        @NotBlank(message = "Slug is required")
        @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must contain only lowercase letters, numbers, and hyphens")
        @Size(min = 3, max = 50, message = "Slug must be between 3 and 50 characters")
        String slug,

        @Size(max = 500, message = "Description cannot exceed 500 characters")
        String description,

        @Email(message = "Invalid email format")
        @Size(max = 255, message = "Email cannot exceed 255 characters")
        String email,

        @Size(max = 2048, message = "Logo URL cannot exceed 2048 characters")
        String logo,

        // Validation for metadata can be added as needed depending on business logic
        Map<String, Object> metadata
) {
}
