package com.lagab.eventz.app.domain.ticket.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.lagab.eventz.app.domain.ticket.dto.TicketDTO;
import com.lagab.eventz.app.domain.ticket.entity.Ticket;

@Mapper(componentModel = "spring")
public interface TicketMapper {

    @Mapping(target = "eventName", source = "event.name")
    @Mapping(target = "surtitle", source = "event.surtitle")
    @Mapping(target = "subtitle", source = "event.subtitle")
    @Mapping(target = "startDate", source = "event.startDate")
    @Mapping(target = "endDate", source = "event.endDate")
    @Mapping(target = "venueName", source = "event.address.name")
    @Mapping(target = "venueAddress", source = "event.address.address1")
    @Mapping(target = "venueCity", source = "event.address.city")
    @Mapping(target = "venueCountry", source = "event.address.country")
    @Mapping(target = "buyerName", source = "order.billingName")
    @Mapping(target = "ticketType", source = "ticketType.name")
    @Mapping(target = "ticketNumber", source = "ticketCode")
    @Mapping(target = "qrCode", source = "qrCode")
    @Mapping(target = "barcodeNumber", ignore = true)
    @Mapping(target = "organizerName", source = "event.organizer.fullName")
    @Mapping(target = "organizerPhone", ignore = true)
    @Mapping(target = "organizerEmail", source = "event.organizer.email")
    @Mapping(target = "organizerWebsite", source = "event.website")
    @Mapping(target = "orderNumber", source = "order.orderNumber")
    @Mapping(target = "orderDate", source = "order.createdAt")
    @Mapping(target = "ticketId", source = "id", qualifiedByName = "idToString")
    @Mapping(target = "price", source = "ticketType.price", qualifiedByName = "priceToDouble")
    TicketDTO toDto(Ticket ticket);

    @Named("idToString")
    default String idToString(Long id) {
        return id == null ? null : String.valueOf(id);
    }

    @Named("priceToDouble")
    default double priceToDouble(java.math.BigDecimal price) {
        return price == null ? 0.0 : price.doubleValue();
    }
}
