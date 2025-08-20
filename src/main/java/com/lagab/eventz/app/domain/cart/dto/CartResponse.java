package com.lagab.eventz.app.domain.cart.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

    public boolean isEmpty() {
        return items == null || items.isEmpty();
    }

    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }

    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    public List<CartMessage> getAllMessages() {
        List<CartMessage> allMessages = new ArrayList<>();
        if (errors != null)
            allMessages.addAll(errors);
        if (warnings != null)
            allMessages.addAll(warnings);
        return allMessages;
    }
}
