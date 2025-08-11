package com.lagab.eventz.app.domain.event.dto.ticket.category;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTicketCategoryRequest(
        @NotBlank(message = "Category name is required")
        @Size(max = 100, message = "Name cannot exceed 100 characters")
        String name,

        @Size(max = 500, message = "Description cannot exceed 500 characters")
        String description,

        @Min(value = 0, message = "Display order must be positive or zero")
        @Max(value = 999, message = "Display order cannot exceed 999")
        Integer displayOrder,

        Boolean isActive,

        Boolean isCollapsed
) {
    public CreateTicketCategoryRequest {
        if (displayOrder == null) {
            displayOrder = 0;
        }
        if (isActive == null) {
            isActive = true;
        }
        if (isCollapsed == null) {
            isCollapsed = false;
        }
    }
}
