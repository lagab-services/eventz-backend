package com.lagab.eventz.app.domain.event.dto;

import java.time.LocalDateTime;

import com.lagab.eventz.app.domain.event.model.EventStatus;
import com.lagab.eventz.app.domain.event.model.EventType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

public record UpdateEventDTO(
        @Size(max = 255, message = "Event name must not exceed 255 characters")
        String name,

        @Size(max = 2000, message = "Description must not exceed 2000 characters")
        String description,

        @Size(max = 500, message = "Summary must not exceed 500 characters")
        String summary,

        @Size(max = 100, message = "Surtitle must not exceed 100 characters")
        String surtitle,

        @Size(max = 100, message = "Subtitle must not exceed 100 characters")
        String subtitle,

        LocalDateTime startDate,
        LocalDateTime endDate,
        LocalDateTime registrationStart,
        LocalDateTime registrationEnd,
        EventStatus status,
        EventType type,
        String imageUrl,
        String website,
        Integer maxAttendees,
        Boolean isPublic,
        Boolean isFree,
        String currency,

        @Valid
        UpdateAddressDTO address
) {
}
