package com.lagab.eventz.app.domain.ticket.dto;

import java.util.Map;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AttendeeInfo(
        @NotBlank(message = "First name is required")
        @Size(min = 1, max = 50)
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(min = 1, max = 50)
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,
        @NotNull(message = "Ticket type is required")
        Long ticketTypeId,

        // Custom fields
        Map<String, String> customFields
) {
}
