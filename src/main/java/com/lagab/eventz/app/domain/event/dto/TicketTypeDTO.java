package com.lagab.eventz.app.domain.event.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TicketTypeDTO(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Integer quantityAvailable,
        Integer quantitySold,
        LocalDateTime saleStart,
        LocalDateTime saleEnd,
        Integer minQuantity,
        Integer maxQuantity,
        Boolean isActive,
        Integer remainingTickets
) {
}
