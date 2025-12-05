package com.lagab.eventz.app.domain.order.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.data.domain.Page;

import com.lagab.eventz.app.domain.event.model.Address;
import com.lagab.eventz.app.domain.event.model.Event;
import com.lagab.eventz.app.domain.event.util.AddressUtil;
import com.lagab.eventz.app.domain.order.dto.OrderResponse;
import com.lagab.eventz.app.domain.order.model.Order;
import com.lagab.eventz.app.domain.order.model.OrderItem;
import com.lagab.eventz.app.domain.ticket.mapper.TicketMapper;
import com.lagab.eventz.app.util.UrlUtil;

@Mapper(componentModel = "spring", uses = { TicketMapper.class })
public interface OrderMapper {

    @Mapping(target = "orderId", source = "id")
    @Mapping(target = "items", source = "orderItems")
    @Mapping(target = "eventTitle", source = "event.name")
    @Mapping(target = "eventUrl", source = "event", qualifiedByName = "toEventUrl")
    @Mapping(target = "eventStartDate", source = "event.startDate")
    @Mapping(target = "eventEndDate", source = "event.endDate")
    @Mapping(target = "eventLocation", source = "event.address.name")
    @Mapping(target = "eventAddress", source = "event.address", qualifiedByName = "formatAddress")
    @Mapping(target = "expiresAt", source = ".", qualifiedByName = "computeExpiresAt")
    @Mapping(target = "tickets", source = "tickets")
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

    @Named("toEventUrl")
    default String toEventUrl(Event event) {
        return UrlUtil.slugify(event.getName()) + "_E" + event.getId();
    }

    @Named("formatAddress")
    default String formatAddress(Address address) {
        return AddressUtil.formatAddress(address);
    }
}
