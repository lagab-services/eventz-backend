package com.lagab.eventz.app.domain.event.mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.lagab.eventz.app.domain.event.dto.ticket.CreateTicketTypeRequest;
import com.lagab.eventz.app.domain.event.dto.ticket.TicketTypeDTO;
import com.lagab.eventz.app.domain.event.dto.ticket.TicketTypeStatsDTO;
import com.lagab.eventz.app.domain.event.dto.ticket.UpdateTicketTypeRequest;
import com.lagab.eventz.app.domain.event.model.TicketType;
import com.lagab.eventz.app.domain.event.projection.TicketTypeStatsProjection;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        imports = { LocalDateTime.class }
)
public interface TicketTypeMapper {

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "quantityRemaining", source = ".", qualifiedByName = "calculateQuantityRemaining")
    @Mapping(target = "totalPrice", source = ".", qualifiedByName = "calculateTotalPrice")
    @Mapping(target = "isOnSale", source = ".", qualifiedByName = "calculateIsOnSale")
    @Mapping(target = "isSoldOut", source = ".", qualifiedByName = "calculateIsSoldOut")
    @Mapping(target = "eventId", source = "event.id")
    @Mapping(target = "eventName", source = "event.name")
    TicketTypeDTO toDTO(TicketType ticketType);

    List<TicketTypeDTO> toDTOList(List<TicketType> ticketTypes);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "quantitySold", constant = "0")
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "category", ignore = true)
    TicketType toEntity(CreateTicketTypeRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "quantitySold", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "category", ignore = true)
    void updateEntityFromDTO(UpdateTicketTypeRequest request, @MappingTarget TicketType ticketType);

    // Méthodes par défaut pour les calculs

    @Named("calculateQuantityRemaining")
    default Integer calculateQuantityRemaining(TicketType ticketType) {
        if (ticketType == null || ticketType.getQuantityAvailable() == null) {
            return null;
        }
        int sold = ticketType.getQuantitySold() != null ? ticketType.getQuantitySold() : 0;
        return ticketType.getQuantityAvailable() - sold;
    }

    @Named("calculateTotalPrice")
    default BigDecimal calculateTotalPrice(TicketType ticketType) {
        if (ticketType == null || ticketType.getPrice() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal fee = ticketType.getFee() != null ? ticketType.getFee() : BigDecimal.ZERO;
        return ticketType.getPrice().add(fee);
    }

    @Named("calculateIsOnSale")
    default boolean calculateIsOnSale(TicketType ticketType) {
        if (ticketType == null || !Boolean.TRUE.equals(ticketType.getIsActive())) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        boolean afterStart = ticketType.getSaleStart() == null || !ticketType.getSaleStart().isAfter(now);
        boolean beforeEnd = ticketType.getSaleEnd() == null || !ticketType.getSaleEnd().isBefore(now);
        return afterStart && beforeEnd;
    }

    @Named("calculateIsSoldOut")
    default boolean calculateIsSoldOut(TicketType ticketType) {
        if (ticketType == null || ticketType.getQuantityAvailable() == null) {
            return false;
        }
        int sold = ticketType.getQuantitySold() != null ? ticketType.getQuantitySold() : 0;
        return ticketType.getQuantityAvailable() <= sold;
    }

    @Mapping(target = "sellThroughRate", source = ".", qualifiedByName = "calculateSellThroughRate")
    @Mapping(target = "totalRevenue", source = ".", qualifiedByName = "safeTotalRevenue")
    @Mapping(target = "averagePrice", source = ".", qualifiedByName = "safeAveragePrice")
    TicketTypeStatsDTO toDTO(TicketTypeStatsProjection projection);

    @Named("calculateSellThroughRate")
    default Double calculateSellThroughRate(TicketTypeStatsProjection projection) {
        if (projection == null || projection.getTotalCapacity() == null || projection.getTotalCapacity() == 0) {
            return 0.0;
        }
        int totalSold = projection.getTotalSold() != null ? projection.getTotalSold() : 0;
        return (double) totalSold / projection.getTotalCapacity() * 100;
    }

    @Named("safeTotalRevenue")
    default BigDecimal safeTotalRevenue(TicketTypeStatsProjection projection) {
        return projection != null && projection.getTotalRevenue() != null
                ? projection.getTotalRevenue()
                : BigDecimal.ZERO;
    }

    @Named("safeAveragePrice")
    default Double safeAveragePrice(TicketTypeStatsProjection projection) {
        return projection != null && projection.getAveragePrice() != null
                ? projection.getAveragePrice()
                : 0.0;
    }
}
