package com.lagab.eventz.app.domain.event.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Represents a ticket type with all its properties and computed fields")
public record TicketTypeDTO(
        @Schema(description = "Unique identifier of the ticket type", example = "123")
        Long id,

        @Schema(description = "Name of the ticket type", example = "VIP Pass", required = true)
        String name,

        @Schema(description = "Description of the ticket type", example = "Access to VIP lounge and premium seating")
        String description,

        @Schema(description = "Price of the ticket", example = "199.99", required = true)
        BigDecimal price,

        @Schema(description = "Number of tickets available for purchase", example = "100")
        Integer quantityAvailable,

        @Schema(description = "Number of tickets already sold", example = "50")
        Integer quantitySold,

        @Schema(description = "Date and time when ticket sales start", example = "2023-12-01T09:00:00")
        LocalDateTime saleStart,

        @Schema(description = "Date and time when ticket sales end", example = "2023-12-24T23:59:59")
        LocalDateTime saleEnd,

        @Schema(description = "Minimum quantity of tickets that can be purchased in a single order", example = "1")
        Integer minQuantity,

        @Schema(description = "Maximum quantity of tickets that can be purchased in a single order", example = "10")
        Integer maxQuantity,

        @Schema(description = "Indicates if the ticket type is currently active", example = "true")
        Boolean isActive,

        @Schema(description = "DEPRECATED - Use quantityRemaining instead")
        Integer remainingTickets,

        // Computed fields
        @Schema(description = "Calculated number of tickets remaining (quantityAvailable - quantitySold)", example = "50")
        Integer quantityRemaining,

        @Schema(description = "Calculated total price for the ticket type (price * quantitySold)", example = "9999.50")
        BigDecimal totalPrice,

        @Schema(description = "Indicates if tickets are currently on sale (based on sale dates and availability)", example = "true")
        Boolean isOnSale,

        @Schema(description = "Indicates if all tickets of this type have been sold out", example = "false")
        Boolean isSoldOut
) {
}
