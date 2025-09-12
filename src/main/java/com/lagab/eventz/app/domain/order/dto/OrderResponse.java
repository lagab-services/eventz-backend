package com.lagab.eventz.app.domain.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.lagab.eventz.app.domain.order.model.OrderStatus;

/**
 * Record representing the response when creating an order
 */
public record OrderResponse(
        Long orderId,
        String orderNumber,
        OrderStatus status,
        BigDecimal totalAmount,
        BigDecimal feesAmount,
        LocalDateTime createdAt,

        // Ticket details
        List<OrderItemResponse> items,

        // Event information
        String eventTitle,
        LocalDateTime eventDate,
        String eventLocation,

        // Next steps
        LocalDateTime expiresAt, // Reservation expiration
        String notes
) {

    /**
     * Record for order items
     */
    public record OrderItemResponse(
            String ticketTypeName,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal totalPrice
    ) {
    }
}
