package com.lagab.eventz.app.domain.ticket.dto;

import java.time.LocalDateTime;

import com.lagab.eventz.app.domain.ticket.entity.CheckInStatus;

public record AttendeeSearchCriteria(
        Long eventId,
        String name,
        String email,
        CheckInStatus checkInStatus,
        Long ticketTypeId,
        LocalDateTime checkedInAfter,
        LocalDateTime checkedInBefore
) {
}
