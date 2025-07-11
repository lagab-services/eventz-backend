package com.lagab.eventz.app.domain.event.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.lagab.eventz.app.domain.event.dto.TicketTypeDTO;
import com.lagab.eventz.app.domain.ticket.entity.TicketType;

@Mapper(componentModel = "spring")
public abstract class TicketTypeMapper {

    @Mapping(target = "remainingTickets", source = "ticketType", qualifiedByName = "calculateRemainingTickets")
    public abstract TicketTypeDTO toDto(TicketType ticketType);

    public abstract List<TicketTypeDTO> toDtoList(List<TicketType> ticketTypes);

    @Named("calculateRemainingTickets")
    public Integer calculateRemainingTickets(TicketType ticketType) {
        if (ticketType.getQuantityAvailable() == null) {
            return null;
        }
        int sold = ticketType.getQuantitySold() != null ? ticketType.getQuantitySold() : 0;
        return Math.max(0, ticketType.getQuantityAvailable() - sold);
    }
}
