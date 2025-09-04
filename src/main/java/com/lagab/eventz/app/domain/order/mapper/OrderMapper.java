package com.lagab.eventz.app.domain.order.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.data.domain.Page;

import com.lagab.eventz.app.domain.order.dto.OrderResponse;
import com.lagab.eventz.app.domain.order.model.Order;
import com.lagab.eventz.app.domain.order.model.OrderItem;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "orderId", source = "id")
    @Mapping(target = "items", source = "orderItems")
    @Mapping(target = "eventTitle", source = "event.name")
    @Mapping(target = "eventDate", source = "event.startDate")
    @Mapping(target = "eventLocation", source = "event.address.address1")
    @Mapping(target = "expiresAt", source = ".", qualifiedByName = "computeExpiresAt")
    OrderResponse toResponse(Order order);

    @Mapping(target = "ticketTypeName", source = "ticketType.name")
    @Mapping(target = "quantity", source = "quantity")
    @Mapping(target = "unitPrice", source = "unitPrice")
    @Mapping(target = "totalPrice", source = "totalPrice")
    OrderResponse.OrderItemResponse toResponse(OrderItem item);

    List<OrderResponse.OrderItemResponse> toItemResponses(List<OrderItem> items);

    default Page<OrderResponse> toResponse(Page<Order> orders) {
        return orders.map(this::toResponse);
    }

    @Named("computeExpiresAt")
    default LocalDateTime computeExpiresAt(Order order) {
        if (order == null)
            return null;
        // Prefer a defined payment deadline; else fallback to 5 minutes after creation (matches existing controller behavior)
        if (order.getPaymentDeadline() != null) {
            return order.getPaymentDeadline();
        }
        if (order.getCreatedAt() != null) {
            return order.getCreatedAt().plusMinutes(5);
        }
        return LocalDateTime.now().plusMinutes(5);
    }
}
