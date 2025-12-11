package com.lagab.eventz.app.domain.ticket.dto;

import java.util.Map;

public record AttendeeStatistics(
        long totalAttendees,
        long checkedIn,
        long cancelled,
        Map<String, Long> attendeesByTicketType
) {
}
