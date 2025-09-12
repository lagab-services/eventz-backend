package com.lagab.eventz.app.domain.ticket.dto;

import java.util.Map;

import com.lagab.eventz.app.domain.ticket.entity.CheckInStatus;

public record AttendeeResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String ticketNumber,
        String ticketTypeName,
        CheckInStatus checkInStatus,
        Map<String, String> customFields
) {
}
