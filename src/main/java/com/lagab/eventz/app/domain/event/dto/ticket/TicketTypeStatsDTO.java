package com.lagab.eventz.app.domain.event.dto.ticket;

import java.math.BigDecimal;

public record TicketTypeStatsDTO(
        Long totalTicketTypes,
        Long activeTicketTypes,
        Long soldOutTicketTypes,
        Integer totalCapacity,
        Integer totalSold,
        Integer totalRemaining,
        BigDecimal totalRevenue,
        Double averagePrice,
        Double sellThroughRate
) {
}
