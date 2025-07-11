package com.lagab.eventz.app.domain.event.dto;

import java.time.LocalDateTime;

import com.lagab.eventz.app.domain.event.model.EventStatus;
import com.lagab.eventz.app.domain.event.model.EventType;

public record EventSearchDTO(
        String keyword,
        EventType type,
        EventStatus status,
        String city,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Boolean isFree,
        Double latitude,
        Double longitude,
        Double radius,
        Long organizerId
) {

    public EventSearchDTO() {
        this(null, null, null, null, null, null, null, null, null, null, null);
    }
}
