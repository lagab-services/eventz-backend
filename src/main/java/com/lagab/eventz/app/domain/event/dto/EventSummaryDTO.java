package com.lagab.eventz.app.domain.event.dto;

import java.time.LocalDateTime;

import com.lagab.eventz.app.domain.event.model.EventStatus;
import com.lagab.eventz.app.domain.event.model.EventType;

public record EventSummaryDTO(
        Long id,
        String name,
        String summary,
        LocalDateTime startDate,
        LocalDateTime endDate,
        EventStatus status,
        EventType type,
        String imageUrl,
        Boolean isPublic,
        Boolean isFree,
        String currency,
        String city,
        String country,
        Double averageRating,
        Long reviewCount,
        Integer availableTickets
) {
}
