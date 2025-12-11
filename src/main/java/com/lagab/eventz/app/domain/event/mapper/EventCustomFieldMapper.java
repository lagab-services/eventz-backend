package com.lagab.eventz.app.domain.event.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.lagab.eventz.app.domain.event.dto.customfield.EventCustomFieldDTO;
import com.lagab.eventz.app.domain.event.dto.customfield.EventCustomFieldRequest;
import com.lagab.eventz.app.domain.event.model.EventCustomField;

@Mapper(componentModel = "spring")
public interface EventCustomFieldMapper {

    // Entity -> DTO
    @Mapping(target = "isRequired", source = "required")
    @Mapping(target = "ticketTypeId", source = "ticketType.id")
    EventCustomFieldDTO toDto(EventCustomField entity);

    // Request -> Entity (for create). Event and TicketType are set in service.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "ticketType", ignore = true)
    @Mapping(target = "required", source = "isRequired")
    EventCustomField fromRequest(EventCustomFieldRequest request);

    // Update allowed fields only (do not update fieldName or relations)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "ticketType", ignore = true)
    @Mapping(target = "fieldName", ignore = true)
    @Mapping(target = "required", source = "isRequired")
    void updateEntityFromRequest(EventCustomFieldRequest request, @MappingTarget EventCustomField entity);
}
