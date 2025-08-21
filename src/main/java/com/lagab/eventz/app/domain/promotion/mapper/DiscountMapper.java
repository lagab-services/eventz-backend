package com.lagab.eventz.app.domain.promotion.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import com.lagab.eventz.app.domain.promotion.dto.DiscountDto;
import com.lagab.eventz.app.domain.promotion.dto.DiscountResponse;
import com.lagab.eventz.app.domain.promotion.model.Discount;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DiscountMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "ticketCategory", ignore = true)
    @Mapping(target = "ticketTypes", ignore = true)
    @Mapping(target = "quantitySold", ignore = true)
    @Mapping(target = "amountOff", source = "amount_off")
    @Mapping(target = "percentOff", source = "percent_off")
    @Mapping(target = "quantityAvailable", source = "quantity_available")
    @Mapping(target = "startDate", source = "start_date")
    @Mapping(target = "endDate", source = "end_date")
    @Mapping(target = "endDateRelativeSeconds", source = "end_date_relative")
    Discount toEntity(DiscountDto dto);

    @Mapping(target = "eventId", source = "event.id")
    @Mapping(target = "ticket_category_id", source = "ticketCategory.id")
    @Mapping(target = "ticket_type_ids", expression = "java(entity.getTicketTypes() != null ? entity.getTicketTypes().stream().map(t -> t.getId()).collect(java.util.stream.Collectors.toList()) : null)")
    @Mapping(target = "amount_off", source = "amountOff")
    @Mapping(target = "percent_off", source = "percentOff")
    @Mapping(target = "quantity_available", source = "quantityAvailable")
    @Mapping(target = "quantity_sold", source = "quantitySold")
    @Mapping(target = "start_date", source = "startDate")
    @Mapping(target = "end_date", source = "endDate")
    @Mapping(target = "end_date_relative", source = "endDateRelativeSeconds")
    DiscountResponse toResponse(Discount entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "ticketCategory", ignore = true)
    @Mapping(target = "ticketTypes", ignore = true)
    @Mapping(target = "quantitySold", ignore = true)
    @Mapping(target = "amountOff", source = "amount_off")
    @Mapping(target = "percentOff", source = "percent_off")
    @Mapping(target = "quantityAvailable", source = "quantity_available")
    @Mapping(target = "startDate", source = "start_date")
    @Mapping(target = "endDate", source = "end_date")
    @Mapping(target = "endDateRelativeSeconds", source = "end_date_relative")
    void updateEntity(DiscountDto dto, @MappingTarget Discount entity);

}
