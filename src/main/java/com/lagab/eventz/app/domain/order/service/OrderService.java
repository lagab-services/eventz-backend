package com.lagab.eventz.app.domain.order.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.lagab.eventz.app.domain.cart.dto.CartMessage;
import com.lagab.eventz.app.domain.cart.model.Cart;
import com.lagab.eventz.app.domain.cart.model.CartItem;
import com.lagab.eventz.app.domain.cart.model.CartValidationResult;
import com.lagab.eventz.app.domain.cart.service.CartService;
import com.lagab.eventz.app.domain.event.dto.ticket.TicketTypeDTO;
import com.lagab.eventz.app.domain.event.mapper.TicketTypeMapper;
import com.lagab.eventz.app.domain.event.model.Event;
import com.lagab.eventz.app.domain.event.model.TicketType;
import com.lagab.eventz.app.domain.event.repository.TicketTypeRepository;
import com.lagab.eventz.app.domain.event.service.EventService;
import com.lagab.eventz.app.domain.order.dto.OrderRequest;
import com.lagab.eventz.app.domain.order.dto.OrderResponse;
import com.lagab.eventz.app.domain.order.exception.OrderException;
import com.lagab.eventz.app.domain.order.mapper.OrderMapper;
import com.lagab.eventz.app.domain.order.model.Order;
import com.lagab.eventz.app.domain.order.model.OrderItem;
import com.lagab.eventz.app.domain.order.model.OrderStatus;
import com.lagab.eventz.app.domain.order.repository.OrderRepository;
import com.lagab.eventz.app.domain.payment.model.Payment;
import com.lagab.eventz.app.domain.payment.service.PaymentService;
import com.lagab.eventz.app.domain.payment.service.StripeService;
import com.lagab.eventz.app.domain.ticket.dto.AttendeeInfo;
import com.lagab.eventz.app.domain.ticket.entity.Ticket;
import com.lagab.eventz.app.domain.ticket.entity.TicketStatus;
import com.lagab.eventz.app.domain.ticket.service.AttendeeService;
import com.lagab.eventz.app.domain.ticket.service.TicketService;
import com.lagab.eventz.app.domain.user.model.User;
import com.lagab.eventz.app.domain.user.service.UserService;
import com.stripe.model.checkout.Session;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;

    private final TicketTypeRepository ticketTypeRepository;

    private final TicketTypeMapper ticketTypeMapper;

    private final CartService cartService;

    private final TicketService ticketService;

    private final PaymentService paymentService;
    private final EventService eventService;
    private final UserService userService;
    private final StripeService stripeService;
    private final AttendeeService attendeeService;
    private final OrderMapper orderMapper;

    public Session createCheckoutSession(String sessionId, Long userId, OrderRequest request) {

        Order order = createOrderFromCart(sessionId, userId, request);
        order.setPaymentDeadline(LocalDateTime.now().plusMinutes(15));

        // Create stripe Checkout session
        return stripeService.createCheckoutSession(order, request, sessionId);
    }

    public Order createOrderFromCart(String sessionId, Long userId, OrderRequest request) {
        // Retrieve and validate the cart
        CartValidationResult validationResult = cartService.validateAndRefreshCart(sessionId, userId);
        Cart cart = validationResult.getCart();

        if (!validationResult.isValid()) {
            String errorMessages = validationResult.getErrors().stream()
                                                   .map(CartMessage::getDefaultMessage)
                                                   .collect(Collectors.joining(", "));
            throw new OrderException("The cart contains errors: " + errorMessages);
        }

        if (cart.isEmpty()) {
            throw new OrderException("The cart is empty");
        }
        User loggedUser = null;
        try {
            loggedUser = getUserById(userId);
        } catch (Exception e) {
            log.debug(e.getMessage());
        }

        // Create the order
        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setUser(loggedUser);
        order.setTotalAmount(cart.getTotal());
        order.setFeesAmount(cart.getFees());
        order.setStatus(OrderStatus.PENDING);
        order.setBillingEmail(request.billingEmail());
        order.setBillingName(request.billingName());
        order.setDiscountAmount(cart.getDiscount());
        order.setPromoCode(cart.getPromoCode());
        order.setNotes(request.notes());

        // Set the event (we take the first event from the cart)
        Long eventId = cart.getItems().getFirst().getEventId();
        order.setEvent(getEventById(eventId));

        // Create order items
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cart.getItems()) {
            TicketType ticketType = ticketTypeRepository.findById(cartItem.getTicketTypeId())
                                                        .orElseThrow(() -> new OrderException("Ticket type not found"));

            // Verify availability one last time
            validateTicketAvailability(ticketTypeMapper.toDTO(ticketType), cartItem.getQuantity());

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setTicketType(ticketType);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(cartItem.getUnitPrice());
            orderItem.setTotalPrice(cartItem.getTotalPrice());

            orderItems.add(orderItem);
        }

        order.setOrderItems(orderItems);

        // Save the order
        Order savedOrder = orderRepository.save(order);

        // Build attendees
        for (AttendeeInfo attendeeInfo : request.attendees()) {
            attendeeService.createAttendee(attendeeInfo, order);
        }

        // Update sold quantities
        updateTicketQuantities(orderItems);

        return savedOrder;
    }

    public Order finalizeOrder(Long orderId, Session stripeSession) {
        log.info("Finalizing order {} after successful payment", orderId);

        Order order = getOrderById(orderId);

        if (order.getStatus() != OrderStatus.PENDING) {
            log.warn("Order {} is not in pending status, current status: {}", orderId, order.getStatus());
            throw new OrderException("Order is not in pending status");
        }

        // Create payment record
        Payment payment = paymentService.buildPaymentSuccess(order, stripeSession);
        order.getPayments().add(payment);

        // Confirm the order
        order.setStatus(OrderStatus.PAID);
        order.setUpdatedAt(LocalDateTime.now());

        // Generate tickets
        List<Ticket> tickets = ticketService.generateTicketsForOrder(order);
        order.setTickets(tickets);

        // Clear the cart
        if (order.getUser() != null) {
            cartService.clearCart(null, order.getUser().getId());
        } else {
            String sessionId = stripeSession.getMetadata().get("sessionId");
            cartService.clearCart(sessionId, null);
        }

        orderRepository.save(order);

        Order savedOrder = orderRepository.save(order);

        // Send confirmation email (to be implemented)
        // emailService.sendOrderConfirmation(savedOrder);

        log.info("Order {} finalized successfully", order.getOrderNumber());

        return savedOrder;
    }

    public void abortOrder(Long orderId) {
        Order order = getOrderById(orderId);

        if (order.getStatus() != OrderStatus.PENDING) {
            log.warn("Order {} is not in pending status to be aborted, current status: {}", orderId, order.getStatus());
            throw new OrderException("Order is not in pending status to be aborted");
        }
        abortOrder(order);
    }

    public void expireOrder(Long orderId) {
        log.info("Expiring order {}", orderId);

        Order order = getOrderById(orderId);

        if (order.getStatus() == OrderStatus.PENDING) {
            // Release reserved stock
            releaseTicketQuantities(order.getOrderItems());

            // Mark as expired
            order.setStatus(OrderStatus.EXPIRED);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);

            log.info("Order {} expired and stock released", order.getOrderNumber());
        } else {
            log.warn("Cannot expire order {} with status {}", orderId, order.getStatus());
        }
    }

    private void abortOrder(Order order) {
        // Release reserved quantities
        releaseTicketQuantities(order.getOrderItems());
        order.setStatus(OrderStatus.ABORTED);
        orderRepository.save(order);
    }

    public Order cancelOrder(Long orderId, String reason) {
        Order order = getOrderById(orderId);

        if (order.getStatus() == OrderStatus.PAID) {
            // Process refund if necessary
            paymentService.refundPayment(order);
        }

        // Release quantities
        releaseTicketQuantities(order.getOrderItems());

        // Cancel tickets
        if (order.getTickets() != null) {
            order.getTickets().forEach(ticket -> ticket.setStatus(TicketStatus.CANCELLED));
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        // add reason in order notes
        StringBuilder notes = new StringBuilder();
        if (StringUtils.hasText(order.getNotes())) {
            notes.append(order.getNotes()).append(System.lineSeparator());
        }
        if (StringUtils.hasText(reason)) {
            notes.append(reason.trim());
        }
        order.setNotes(notes.toString());

        return orderRepository.save(order);
    }

    @Transactional
    public OrderResponse cancelOrderResponse(Long orderId, String reason) {
        return orderMapper.toResponse(cancelOrder(orderId, reason));
    }

    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                              .orElseThrow(() -> new OrderException("Order not found"));
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderResponseById(Long orderId) {
        return orderMapper.toResponse(getOrderById(orderId));
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByUser(Long userId, Pageable pageable) {
        Page<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return orders.map(orderMapper::toResponse);
    }

    public Page<OrderResponse> getOrdersByEvent(Long eventId, Pageable pageable) {
        Page<Order> orders = orderRepository.findByEventIdOrderByCreatedAtDesc(eventId, pageable);
        return orders.map(orderMapper::toResponse);
    }

    private String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis() + "-" +
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void validateTicketAvailability(TicketTypeDTO ticketType, Integer quantity) {
        Integer available = ticketType.remainingTickets();
        if (available != null && quantity > available) {
            throw new OrderException("Only " + available + " tickets available for " + ticketType.name());
        }
    }

    private void updateTicketQuantities(List<OrderItem> orderItems) {
        for (OrderItem item : orderItems) {
            TicketType ticketType = item.getTicketType();
            ticketType.setQuantitySold(ticketType.getQuantitySold() + item.getQuantity());
            ticketTypeRepository.save(ticketType);
        }
    }

    private void releaseTicketQuantities(List<OrderItem> orderItems) {
        for (OrderItem item : orderItems) {
            TicketType ticketType = item.getTicketType();
            ticketType.setQuantitySold(Math.max(0, ticketType.getQuantitySold() - item.getQuantity()));
            ticketTypeRepository.save(ticketType);
        }
    }

    // Utility methods (to be implemented according to your architecture)
    private User getUserById(Long userId) {
        return userService.findById(userId);
    }

    private Event getEventById(Long eventId) {
        return eventService.findEventById(eventId);
    }

}
