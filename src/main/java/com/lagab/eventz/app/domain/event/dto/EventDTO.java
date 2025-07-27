package com.lagab.eventz.app.domain.event.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.lagab.eventz.app.domain.event.dto.ticket.TicketTypeDTO;
import com.lagab.eventz.app.domain.event.model.EventStatus;
import com.lagab.eventz.app.domain.event.model.EventType;

public record EventDTO(
        Long id,
        String name,
        String description,
        String summary,
        String surtitle,
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
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long organizerId,
        String organizerName,
        AddressDTO address,
        List<TicketTypeDTO> ticketTypes,
        Double averageRating,
        Long reviewCount
) {
}
