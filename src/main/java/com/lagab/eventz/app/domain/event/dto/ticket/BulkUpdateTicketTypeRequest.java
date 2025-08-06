package com.lagab.eventz.app.domain.event.dto.ticket;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record BulkUpdateTicketTypeRequest(
        @NotEmpty(message = "Update list cannot be empty")
        @Valid
        List<TicketTypeUpdate> updates
) {
    public record TicketTypeUpdate(
            @NotNull(message = "Ticket type ID is required")
            Long id,

            @Valid
            UpdateTicketTypeRequest updateRequest
    ) {
    }
}
