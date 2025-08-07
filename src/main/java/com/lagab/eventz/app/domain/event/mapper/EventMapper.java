package com.lagab.eventz.app.domain.event.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.lagab.eventz.app.domain.event.dto.CreateEventDTO;
import com.lagab.eventz.app.domain.event.dto.EventDTO;
import com.lagab.eventz.app.domain.event.dto.EventSummaryDTO;
import com.lagab.eventz.app.domain.event.dto.UpdateEventDTO;
import com.lagab.eventz.app.domain.event.model.Event;
import com.lagab.eventz.app.domain.org.model.Organization;
import com.lagab.eventz.app.domain.user.model.User;

@Mapper(componentModel = "spring", uses = { AddressMapper.class, TicketTypeMapper.class })
public interface EventMapper {

    @Mapping(source = "organizer.id", target = "organizerId")
    @Mapping(source = "organizer", target = "organizerName", qualifiedByName = "formatOrganizerName")
    @Mapping(source = "organization.id", target = "organizationId")
    @Mapping(source = "organization", target = "organizationName", qualifiedByName = "formatOrganizationName")
    @Mapping(target = "averageRating", ignore = true)
    @Mapping(target = "reviewCount", ignore = true)
    EventDTO toDto(Event event);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "status", constant = "DRAFT")
    @Mapping(target = "organizer", ignore = true)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "ticketTypes", ignore = true)
        //@Mapping(target = "orders", ignore = true)
        //@Mapping(target = "reviews", ignore = true)
    Event toEntity(CreateEventDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "organizer", ignore = true)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "ticketTypes", ignore = true)
    //@Mapping(target = "orders", ignore = true)
    //@Mapping(target = "reviews", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(UpdateEventDTO dto, @MappingTarget Event event);

    @Mapping(source = "address.city", target = "city")
    @Mapping(source = "address.country", target = "country")
    @Mapping(target = "averageRating", ignore = true)
    @Mapping(target = "reviewCount", ignore = true)
    @Mapping(target = "availableTickets", ignore = true)
    EventSummaryDTO toSummaryDto(Event event);

    @Named("formatOrganizerName")
    default String formatOrganizerName(User user) {
        return user.getFullName();
    }

    @Named("formatOrganizationName")
    default String formatOrganizationName(Organization organization) {
        return organization.getName();
    }
}
