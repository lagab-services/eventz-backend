package com.lagab.eventz.app.domain.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.lagab.eventz.app.domain.order.model.OrderStatus;
import com.lagab.eventz.app.domain.ticket.dto.TicketDTO;

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
        String eventUrl,
        LocalDateTime eventStartDate,
        LocalDateTime eventEndDate,
        String eventLocation,
        String eventAddress,

        // Next steps
        LocalDateTime expiresAt, // Reservation expiration
        String notes,
        List<TicketDTO> tickets
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
