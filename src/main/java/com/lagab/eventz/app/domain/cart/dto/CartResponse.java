package com.lagab.eventz.app.domain.cart.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Record representing the current state of the shopping cart
 */
public record CartResponse(
        List<CartItemResponse> items,
        BigDecimal subtotal,
        BigDecimal fees,
        BigDecimal discount,
        BigDecimal total,
        int totalItems,
        LocalDateTime updatedAt,
        String promoCode,

        // Validation information
        boolean isValid,
        List<CartMessage> warnings, // Price changes, reduced quantities, etc.
        List<CartMessage> errors   // Tickets unavailable, etc.
) {

    /**
     * Record for cart items
     */
    public record CartItemResponse(
            Long ticketTypeId,
            String ticketTypeName,
            String eventTitle,
            BigDecimal unitPrice,
            Integer quantity,
            BigDecimal totalPrice,
            Integer availableQuantity,
            boolean isAvailable
    ) {
    }

}
