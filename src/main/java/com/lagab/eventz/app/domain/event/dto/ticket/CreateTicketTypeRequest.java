package com.lagab.eventz.app.domain.event.dto.ticket;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.lagab.eventz.app.common.exception.BusinessException;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTicketTypeRequest(
        @NotBlank(message = "Ticket type name is required")
        @Size(max = 100, message = "Name cannot exceed 100 characters")
        String name,

        @Size(max = 500, message = "Description cannot exceed 500 characters")
        String description,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Price must be positive or zero")
        @Digits(integer = 8, fraction = 2, message = "Price must have a maximum of 8 digits before the decimal and 2 after")
        BigDecimal price,

        @DecimalMin(value = "0.0", inclusive = true, message = "Fees must be positive or zero")
        @Digits(integer = 8, fraction = 2, message = "Fees must have a maximum of 8 digits before the decimal and 2 after")
        BigDecimal fee,

        @Min(value = 0, message = "Sort order must be positive or zero")
        Integer sortOrder,

        @Min(value = 1, message = "Capacity must be at least 1")
        Integer capacity,

        @Min(value = 1, message = "Available quantity must be at least 1")
        Integer quantityAvailable,

        LocalDateTime saleStart,

        @Future(message = "Sale end date must be in the future")
        LocalDateTime saleEnd,

        @Min(value = 1, message = "Minimum quantity must be at least 1")
        Integer minQuantity,

        @Min(value = 1, message = "Maximum quantity must be at least 1")
        Integer maxQuantity,
        
        Long categoryId
) {
    public CreateTicketTypeRequest {
        // Custom validation
        if (fee == null) {
            fee = BigDecimal.ZERO;
        }
        if (minQuantity == null) {
            minQuantity = 1;
        }
        if (maxQuantity == null) {
            maxQuantity = 10;
        }
        if (minQuantity > maxQuantity) {
            throw new BusinessException("Minimum quantity cannot be greater than maximum quantity");
        }
        if (saleStart != null && saleEnd != null && saleStart.isAfter(saleEnd)) {
            throw new BusinessException("Sale start date cannot be after sale end date");
        }
        if (capacity != null && quantityAvailable != null && quantityAvailable > capacity) {
            throw new BusinessException("Available quantity cannot exceed capacity");
        }
    }
}
