package com.lagab.eventz.app.domain.event.dto.ticket.category;

import java.util.List;

import com.lagab.eventz.app.domain.event.dto.ticket.TicketTypeDTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Represents a ticket category with its properties and associated ticket types")
public record TicketCategoryDTO(
        @Schema(description = "Unique identifier of the ticket category", example = "123")
        Long id,

        @Schema(description = "Name of the ticket category", example = "VIP")
        String name,

        @Schema(description = "Description of the ticket category", example = "Premium access tickets")
        String description,

        @Schema(description = "Display order of the category", example = "1")
        Integer displayOrder,

        @Schema(description = "Indicates if the category is currently active", example = "true")
        Boolean isActive,

        @Schema(description = "Indicates if the category is collapsed in the UI", example = "false")
        Boolean isCollapsed,

        @Schema(description = "List of ticket types in this category")
        List<TicketTypeDTO> ticketTypes
) {
}
