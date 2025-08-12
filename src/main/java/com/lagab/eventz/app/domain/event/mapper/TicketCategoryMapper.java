package com.lagab.eventz.app.domain.event.mapper;

import java.util.List;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.lagab.eventz.app.domain.event.dto.ticket.category.CreateTicketCategoryRequest;
import com.lagab.eventz.app.domain.event.dto.ticket.category.TicketCategoryDTO;
import com.lagab.eventz.app.domain.event.dto.ticket.category.UpdateTicketCategoryRequest;
import com.lagab.eventz.app.domain.event.model.TicketCategory;

@Mapper(
        componentModel = "spring",
        uses = { TicketTypeMapper.class },
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface TicketCategoryMapper {

    TicketCategoryDTO toDTO(TicketCategory ticketCategory);

    List<TicketCategoryDTO> toDTOList(List<TicketCategory> ticketCategories);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "ticketTypes", ignore = true)
    TicketCategory toEntity(CreateTicketCategoryRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "ticketTypes", ignore = true)
    void updateEntityFromDTO(UpdateTicketCategoryRequest request, @MappingTarget TicketCategory ticketCategory);
}
