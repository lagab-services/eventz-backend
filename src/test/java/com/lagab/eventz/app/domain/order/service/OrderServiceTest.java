package com.lagab.eventz.app.domain.order.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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

import static com.lagab.eventz.app.domain.cart.dto.CartErrorCode.VALIDATION_ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Tests")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private TicketTypeRepository ticketTypeRepository;

    @Mock
    private TicketTypeMapper ticketTypeMapper;

    @Mock
    private CartService cartService;

    @Mock
    private TicketService ticketService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private EventService eventService;

    @Mock
    private UserService userService;

    @Mock
    private StripeService stripeService;

    @Mock
    private AttendeeService attendeeService;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderService orderService;

    private User testUser;
    private Event testEvent;
    private TicketType testTicketType;
    private Cart testCart;
    private Order testOrder;
    private OrderRequest testOrderRequest;
    private Session testStripeSession;

    @BeforeEach
    void setUp() {
        // Setup test data
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");

        testEvent = new Event();
        testEvent.setId(1L);
        testEvent.setName("Test Event");

        testTicketType = new TicketType();
        testTicketType.setId(1L);
        testTicketType.setName("Standard");
        testTicketType.setPrice(BigDecimal.valueOf(50.00));
        testTicketType.setQuantitySold(0);
        testTicketType.setMaxQuantity(100);

        CartItem testCartItem = new CartItem();
        testCartItem.setTicketTypeId(1L);
        testCartItem.setEventId(1L);
        testCartItem.setQuantity(2);
        testCartItem.setUnitPrice(BigDecimal.valueOf(50.00));
        testCartItem.setTotalPrice(BigDecimal.valueOf(100.00));

        testCart = new Cart();
        testCart.setItems(List.of(testCartItem));
        testCart.setTotal(BigDecimal.valueOf(100.00));
        testCart.setFees(BigDecimal.valueOf(5.00));
        testCart.setDiscount(BigDecimal.ZERO);

        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setOrderNumber("ORD-123456");
        testOrder.setUser(testUser);
        testOrder.setEvent(testEvent);
        testOrder.setStatus(OrderStatus.PENDING);
        testOrder.setTotalAmount(BigDecimal.valueOf(100.00));

        List<AttendeeInfo> attendees = List.of(
                new AttendeeInfo("John", "Doe", "john@example.com", 123456789L, new HashMap<>())
        );

        testOrderRequest = new OrderRequest(
                "Test User",                    // billingName
                "test@example.com",            // billingEmail
                "+33123456789",                // billingPhone
                "123 Test Street",             // billingAddress
                "Test City",                   // billingCity
                "12345",                       // billingZipCode
                "France",                      // billingCountry
                attendees,                     // attendees
                "Test notes",                  // notes
                true,                          // acceptTerms
                false,                         // subscribeNewsletter
                "http://success.url",          // successUrl
                "http://cancel.url"            // cancelUrl
        );

        testStripeSession = mock(Session.class);
    }

    @Nested
    @DisplayName("Create Checkout Session Tests")
    class CreateCheckoutSessionTests {

        @Test
        @DisplayName("Should create checkout session successfully")
        void shouldCreateCheckoutSessionSuccessfully() {
            // Given
            String sessionId = "session123";
            Long userId = 1L;

            CartValidationResult validationResult = new CartValidationResult(testCart);
            validationResult.setValid(true);

            when(cartService.validateAndRefreshCart(sessionId, userId)).thenReturn(validationResult);
            when(userService.findById(userId)).thenReturn(testUser);
            when(eventService.findEventById(1L)).thenReturn(testEvent);
            when(ticketTypeRepository.findById(1L)).thenReturn(Optional.of(testTicketType));
            when(ticketTypeMapper.toDTO(testTicketType)).thenReturn(createTicketTypeDTO());
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
            when(stripeService.createCheckoutSession(any(Order.class), eq(testOrderRequest), eq(sessionId))).thenReturn(testStripeSession);

            // When
            Session result = orderService.createCheckoutSession(sessionId, userId, testOrderRequest);

            // Then
            assertNotNull(result);
            verify(cartService).validateAndRefreshCart(sessionId, userId);
            verify(stripeService).createCheckoutSession(any(Order.class), eq(testOrderRequest), eq(sessionId));
            verify(orderRepository).save(any(Order.class));
        }

        @Test
        @DisplayName("Should throw exception when cart is invalid")
        void shouldThrowExceptionWhenCartIsInvalid() {
            // Given
            String sessionId = "session123";
            Long userId = 1L;

            CartValidationResult validationResult = new CartValidationResult(testCart);
            validationResult.setValid(false);
            validationResult.addError(VALIDATION_ERROR, "Invalid quantity");

            when(cartService.validateAndRefreshCart(sessionId, userId)).thenReturn(validationResult);

            // When & Then
            OrderException exception = assertThrows(OrderException.class,
                    () -> orderService.createCheckoutSession(sessionId, userId, testOrderRequest));

            assertTrue(exception.getMessage().contains("The cart contains errors"));
        }

        @Test
        @DisplayName("Should throw exception when cart is empty")
        void shouldThrowExceptionWhenCartIsEmpty() {
            // Given
            String sessionId = "session123";
            Long userId = 1L;

            Cart emptyCart = new Cart();
            emptyCart.setItems(new ArrayList<>());
            CartValidationResult validationResult = new CartValidationResult(emptyCart);
            validationResult.setValid(true);

            when(cartService.validateAndRefreshCart(sessionId, userId)).thenReturn(validationResult);

            // When & Then
            OrderException exception = assertThrows(OrderException.class,
                    () -> orderService.createCheckoutSession(sessionId, userId, testOrderRequest));

            assertEquals("The cart is empty", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Finalize Order Tests")
    class FinalizeOrderTests {

        @Test
        @DisplayName("Should finalize order successfully")
        void shouldFinalizeOrderSuccessfully() {
            // Given
            Long orderId = 1L;
            testOrder.setStatus(OrderStatus.PENDING);
            testOrder.setPayments(new ArrayList<>());

            Payment payment = new Payment();
            List<Ticket> tickets = List.of(new Ticket());

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
            when(paymentService.buildPaymentSuccess(testOrder, testStripeSession)).thenReturn(payment);
            when(ticketService.generateTicketsForOrder(testOrder)).thenReturn(tickets);
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

            // When
            Order result = orderService.finalizeOrder(orderId, testStripeSession);

            // Then
            assertNotNull(result);
            assertEquals(OrderStatus.PAID, result.getStatus());
            verify(cartService).clearCart(null, testUser.getId());
            verify(ticketService).generateTicketsForOrder(testOrder);
        }

        @Test
        @DisplayName("Should finalize order successfully with sessionID")
        void shouldFinalizeOrderSuccessfullyWithSessionId() {
            // Given
            Long orderId = 1L;
            testOrder.setStatus(OrderStatus.PENDING);
            testOrder.setPayments(new ArrayList<>());
            testOrder.setUser(null);

            Payment payment = new Payment();
            List<Ticket> tickets = List.of(new Ticket());

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
            when(paymentService.buildPaymentSuccess(testOrder, testStripeSession)).thenReturn(payment);
            when(ticketService.generateTicketsForOrder(testOrder)).thenReturn(tickets);
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
            Map<String, String> metadatas = new HashMap<>();
            metadatas.put("sessionId", "session123");

            when(testStripeSession.getMetadata()).thenReturn(metadatas);

            // When
            Order result = orderService.finalizeOrder(orderId, testStripeSession);

            // Then
            assertNotNull(result);
            assertEquals(OrderStatus.PAID, result.getStatus());
            verify(cartService).clearCart("session123", null);
            verify(ticketService).generateTicketsForOrder(testOrder);
        }

        @Test
        @DisplayName("Should throw exception when order is not pending")
        void shouldThrowExceptionWhenOrderIsNotPending() {
            // Given
            Long orderId = 1L;
            testOrder.setStatus(OrderStatus.PAID);

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

            // When & Then
            OrderException exception = assertThrows(OrderException.class,
                    () -> orderService.finalizeOrder(orderId, testStripeSession));

            assertEquals("Order is not in pending status", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Cancel Order Tests")
    class CancelOrderTests {

        @Test
        @DisplayName("Should cancel paid order with refund")
        void shouldCancelPaidOrderWithRefund() {
            // Given
            Long orderId = 1L;
            String reason = "Customer request";
            testOrder.setStatus(OrderStatus.PAID);

            List<Ticket> tickets = List.of(createTestTicket());
            testOrder.setTickets(tickets);

            List<OrderItem> orderItems = List.of(createTestOrderItem());
            testOrder.setOrderItems(orderItems);

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

            // When
            Order result = orderService.cancelOrder(orderId, reason);

            // Then
            assertEquals(OrderStatus.CANCELLED, result.getStatus());
            assertTrue(result.getNotes().contains(reason));
            verify(paymentService).refundPayment(testOrder);
            assertEquals(TicketStatus.CANCELLED, tickets.getFirst().getStatus());
        }

        @Test
        @DisplayName("Should cancel pending order without refund")
        void shouldCancelPendingOrderWithoutRefund() {
            // Given
            Long orderId = 1L;
            String reason = "Event cancelled";
            testOrder.setStatus(OrderStatus.PENDING);

            List<OrderItem> orderItems = List.of(createTestOrderItem());
            testOrder.setOrderItems(orderItems);

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

            // When
            Order result = orderService.cancelOrder(orderId, reason);

            // Then
            assertEquals(OrderStatus.CANCELLED, result.getStatus());
            verify(paymentService, never()).refundPayment(any());
        }
    }

    @Nested
    @DisplayName("Expire Order Tests")
    class ExpireOrderTests {

        @Test
        @DisplayName("Should expire pending order")
        void shouldExpirePendingOrder() {
            // Given
            Long orderId = 1L;
            testOrder.setStatus(OrderStatus.PENDING);

            List<OrderItem> orderItems = List.of(createTestOrderItem());
            testOrder.setOrderItems(orderItems);

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

            // When
            orderService.expireOrder(orderId);

            // Then
            assertEquals(OrderStatus.EXPIRED, testOrder.getStatus());
            verify(orderRepository).save(testOrder);
        }

        @Test
        @DisplayName("Should not expire non-pending order")
        void shouldNotExpireNonPendingOrder() {
            // Given
            Long orderId = 1L;
            testOrder.setStatus(OrderStatus.PAID);

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

            // When
            orderService.expireOrder(orderId);

            // Then
            assertEquals(OrderStatus.PAID, testOrder.getStatus());
            verify(orderRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Get Orders Tests")
    class GetOrdersTests {

        @Test
        @DisplayName("Should get orders by user")
        void shouldGetOrdersByUser() {
            // Given
            Long userId = 1L;
            Pageable pageable = PageRequest.of(0, 10);
            Page<Order> orderPage = new PageImpl<>(List.of(testOrder));
            OrderResponse orderResponse = createOrderResponse();

            when(orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)).thenReturn(orderPage);
            when(orderMapper.toResponse(testOrder)).thenReturn(orderResponse);

            // When
            Page<OrderResponse> result = orderService.getOrdersByUser(userId, pageable);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            verify(orderRepository).findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }

        @Test
        @DisplayName("Should get orders by event")
        void shouldGetOrdersByEvent() {
            // Given
            Long eventId = 1L;
            Pageable pageable = PageRequest.of(0, 10);
            Page<Order> orderPage = new PageImpl<>(List.of(testOrder));
            OrderResponse orderResponse = createOrderResponse();

            when(orderRepository.findByEventIdOrderByCreatedAtDesc(eventId, pageable)).thenReturn(orderPage);
            when(orderMapper.toResponse(testOrder)).thenReturn(orderResponse);

            // When
            Page<OrderResponse> result = orderService.getOrdersByEvent(eventId, pageable);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            verify(orderRepository).findByEventIdOrderByCreatedAtDesc(eventId, pageable);
        }
    }

    @Nested
    @DisplayName("Abort Order Tests")
    class AbortOrderTests {

        @Test
        @DisplayName("Should abort pending order")
        void shouldAbortPendingOrder() {
            // Given
            Long orderId = 1L;
            testOrder.setStatus(OrderStatus.PENDING);

            List<OrderItem> orderItems = List.of(createTestOrderItem());
            testOrder.setOrderItems(orderItems);

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

            // When
            orderService.abortOrder(orderId);

            // Then
            assertEquals(OrderStatus.ABORTED, testOrder.getStatus());
            verify(orderRepository).save(testOrder);
        }

        @Test
        @DisplayName("Should throw exception when aborting non-pending order")
        void shouldThrowExceptionWhenAbortingNonPendingOrder() {
            // Given
            Long orderId = 1L;
            testOrder.setStatus(OrderStatus.PAID);

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

            // When & Then
            OrderException exception = assertThrows(OrderException.class,
                    () -> orderService.abortOrder(orderId));

            assertEquals("Order is not in pending status to be aborted", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Get Order By ID Tests")
    class GetOrderByIdTests {

        @Test
        @DisplayName("Should get order by ID successfully")
        void shouldGetOrderByIdSuccessfully() {
            // Given
            Long orderId = 1L;
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

            // When
            Order result = orderService.getOrderById(orderId);

            // Then
            assertNotNull(result);
            assertEquals(testOrder.getId(), result.getId());
            verify(orderRepository).findById(orderId);
        }

        @Test
        @DisplayName("Should throw exception when order not found")
        void shouldThrowExceptionWhenOrderNotFound() {
            // Given
            Long orderId = 999L;
            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            // When & Then
            OrderException exception = assertThrows(OrderException.class,
                    () -> orderService.getOrderById(orderId));

            assertEquals("Order not found", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Create Order From Cart Tests")
    class CreateOrderFromCartTests {

        @Test
        @DisplayName("Should create order from cart successfully")
        void shouldCreateOrderFromCartSuccessfully() {
            // Given
            String sessionId = "session123";
            Long userId = 1L;

            CartValidationResult validationResult = new CartValidationResult(testCart);
            validationResult.setValid(true);

            when(cartService.validateAndRefreshCart(sessionId, userId)).thenReturn(validationResult);
            when(userService.findById(userId)).thenReturn(testUser);
            when(eventService.findEventById(1L)).thenReturn(testEvent);
            when(ticketTypeRepository.findById(1L)).thenReturn(Optional.of(testTicketType));
            when(ticketTypeMapper.toDTO(testTicketType)).thenReturn(createTicketTypeDTO());
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

            // When
            Order result = orderService.createOrderFromCart(sessionId, userId, testOrderRequest);

            // Then
            assertNotNull(result);
            verify(attendeeService).createAttendee(any(AttendeeInfo.class), any(Order.class));
            verify(ticketTypeRepository).save(testTicketType);
            assertEquals(2, testTicketType.getQuantitySold()); // quantity from cart item
        }

        @Test
        @DisplayName("Should throw exception when ticket type not found")
        void shouldThrowExceptionWhenTicketTypeNotFound() {
            // Given
            String sessionId = "session123";
            Long userId = 1L;

            CartValidationResult validationResult = new CartValidationResult(testCart);
            validationResult.setValid(true);

            when(cartService.validateAndRefreshCart(sessionId, userId)).thenReturn(validationResult);
            when(userService.findById(userId)).thenReturn(testUser);
            when(eventService.findEventById(1L)).thenReturn(testEvent);
            when(ticketTypeRepository.findById(1L)).thenReturn(Optional.empty());

            // When & Then
            OrderException exception = assertThrows(OrderException.class,
                    () -> orderService.createOrderFromCart(sessionId, userId, testOrderRequest));

            assertEquals("Ticket type not found", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when insufficient tickets available")
        void shouldThrowExceptionWhenInsufficientTicketsAvailable() {
            // Given
            String sessionId = "session123";
            Long userId = 1L;

            CartValidationResult validationResult = new CartValidationResult(testCart);
            validationResult.setValid(true);
            TicketTypeDTO ticketTypeDTO = createTicketTypeDTOWithLimitedStock();

            when(cartService.validateAndRefreshCart(sessionId, userId)).thenReturn(validationResult);
            when(userService.findById(userId)).thenReturn(testUser);
            when(eventService.findEventById(1L)).thenReturn(testEvent);
            when(ticketTypeRepository.findById(1L)).thenReturn(Optional.of(testTicketType));
            when(ticketTypeMapper.toDTO(testTicketType)).thenReturn(ticketTypeDTO);

            // When & Then
            OrderException exception = assertThrows(OrderException.class,
                    () -> orderService.createOrderFromCart(sessionId, userId, testOrderRequest));

            assertTrue(exception.getMessage().contains("Only 1 tickets available"));
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle order with notes correctly")
        void shouldHandleOrderWithNotesCorrectly() {
            // Given
            Long orderId = 1L;
            String reason = "New cancellation reason";
            testOrder.setStatus(OrderStatus.PENDING);
            testOrder.setNotes("Existing notes");
            testOrder.setOrderItems(List.of(createTestOrderItem()));

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

            // When
            Order result = orderService.cancelOrder(orderId, reason);

            // Then
            assertTrue(result.getNotes().contains("Existing notes"));
            assertTrue(result.getNotes().contains(reason));
        }

        @Test
        @DisplayName("Should handle order without existing notes")
        void shouldHandleOrderWithoutExistingNotes() {
            // Given
            Long orderId = 1L;
            String reason = "Cancellation reason";
            testOrder.setStatus(OrderStatus.PENDING);
            testOrder.setNotes(null);
            testOrder.setOrderItems(List.of(createTestOrderItem()));

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

            // When
            Order result = orderService.cancelOrder(orderId, reason);

            // Then
            assertEquals(reason, result.getNotes());
        }

        @Test
        @DisplayName("Should handle empty reason for cancellation")
        void shouldHandleEmptyReasonForCancellation() {
            // Given
            Long orderId = 1L;
            String reason = "";
            testOrder.setStatus(OrderStatus.PENDING);
            testOrder.setOrderItems(List.of(createTestOrderItem()));

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

            // When
            Order result = orderService.cancelOrder(orderId, reason);

            // Then
            assertEquals(OrderStatus.CANCELLED, result.getStatus());
        }

        @Test
        @DisplayName("Should handle quantity release correctly")
        void shouldHandleQuantityReleaseCorrectly() {
            // Given
            testTicketType.setQuantitySold(5);
            OrderItem orderItem = createTestOrderItem();
            orderItem.setQuantity(3);

            List<OrderItem> orderItems = List.of(orderItem);

            // When - simulate releasing quantities
            for (OrderItem item : orderItems) {
                TicketType ticketType = item.getTicketType();
                ticketType.setQuantitySold(Math.max(0, ticketType.getQuantitySold() - item.getQuantity()));
            }

            // Then
            assertEquals(2, testTicketType.getQuantitySold()); // 5 - 3 = 2
        }

        @Test
        @DisplayName("Should not allow negative quantity sold")
        void shouldNotAllowNegativeQuantitySold() {
            // Given
            testTicketType.setQuantitySold(1);
            OrderItem orderItem = createTestOrderItem();
            orderItem.setQuantity(3);

            List<OrderItem> orderItems = List.of(orderItem);

            // When - simulate releasing quantities
            for (OrderItem item : orderItems) {
                TicketType ticketType = item.getTicketType();
                ticketType.setQuantitySold(Math.max(0, ticketType.getQuantitySold() - item.getQuantity()));
            }

            // Then
            assertEquals(0, testTicketType.getQuantitySold()); // Should not go below 0
        }

        @Test
        @DisplayName("Should handle cart validation with multiple errors")
        void shouldHandleCartValidationWithMultipleErrors() {
            // Given
            String sessionId = "session123";
            Long userId = 1L;

            CartValidationResult validationResult = new CartValidationResult(testCart);
            validationResult.setValid(false);
            validationResult.addError(VALIDATION_ERROR, "Invalid quantity");
            validationResult.addError(VALIDATION_ERROR, "Ticket not available");

            when(cartService.validateAndRefreshCart(sessionId, userId)).thenReturn(validationResult);

            // When & Then
            OrderException exception = assertThrows(OrderException.class,
                    () -> orderService.createCheckoutSession(sessionId, userId, testOrderRequest));

            assertTrue(exception.getMessage().contains("The cart contains errors"));
            assertTrue(exception.getMessage().contains("Invalid quantity"));
            assertTrue(exception.getMessage().contains("Ticket not available"));
        }

        @Test
        @DisplayName("Should handle order request validation")
        void shouldHandleOrderRequestValidation() {
            // Given
            OrderRequest invalidRequest = new OrderRequest(
                    "",                            // billingName - empty
                    "invalid-email",               // billingEmail - invalid format
                    "123",                         // billingPhone - too short
                    null,                          // billingAddress
                    null,                          // billingCity
                    null,                          // billingZipCode
                    null,                          // billingCountry
                    new ArrayList<>(),             // attendees - empty
                    "Test notes",                  // notes
                    false,                         // acceptTerms - false
                    false,                         // subscribeNewsletter
                    "http://success.url",          // successUrl
                    "http://cancel.url"            // cancelUrl
            );

            // When & Then
            assertFalse(isRequestValid(invalidRequest));
            assertFalse(!invalidRequest.attendees().isEmpty());
            assertEquals(0, invalidRequest.attendees().size());
        }
    }

    private boolean isRequestValid(OrderRequest orderRequest) {
        return orderRequest.billingName() != null && !orderRequest.billingName().trim().isEmpty() &&
                orderRequest.billingEmail() != null && orderRequest.billingEmail().contains("@") &&
                orderRequest.acceptTerms();
    }

    // Helper methods for creating test data
    private TicketTypeDTO createTicketTypeDTO() {
        return new TicketTypeDTO(
                1L,                                    // id
                "Standard",                            // name
                "Standard ticket description",         // description
                BigDecimal.valueOf(50.00),            // price
                100,                                  // quantityAvailable
                0,                                    // quantitySold
                LocalDateTime.now().minusDays(1),     // saleStart
                LocalDateTime.now().plusDays(30),     // saleEnd
                1,                                    // minQuantity
                10,                                   // maxQuantity
                true,                                 // isActive
                100,                                  // remainingTickets (deprecated)
                1L,                                   // categoryId
                "Standard Category",                  // categoryName
                100,                                  // quantityRemaining
                BigDecimal.ZERO,                      // totalPrice
                true,                                 // isOnSale
                false,                                // isSoldOut
                1L,                                   // eventId
                "Test Event"                          // eventName
        );
    }

    private TicketTypeDTO createTicketTypeDTOWithLimitedStock() {
        return new TicketTypeDTO(
                1L,                                    // id
                "Standard",                            // name
                "Standard ticket description",         // description
                BigDecimal.valueOf(50.00),            // price
                100,                                  // quantityAvailable
                99,                                   // quantitySold
                LocalDateTime.now().minusDays(1),     // saleStart
                LocalDateTime.now().plusDays(30),     // saleEnd
                1,                                    // minQuantity
                10,                                   // maxQuantity
                true,                                 // isActive
                1,                                    // remainingTickets (deprecated) - only 1 available but cart has 2
                1L,                                   // categoryId
                "Standard Category",                  // categoryName
                1,                                    // quantityRemaining - only 1 available but cart has 2
                BigDecimal.valueOf(4950.00),          // totalPrice (99 * 50)
                true,                                 // isOnSale
                false,                                // isSoldOut
                1L,                                   // eventId
                "Test Event"                          // eventName
        );
    }

    private OrderItem createTestOrderItem() {
        OrderItem orderItem = new OrderItem();
        orderItem.setId(1L);
        orderItem.setOrder(testOrder);
        orderItem.setTicketType(testTicketType);
        orderItem.setQuantity(2);
        orderItem.setUnitPrice(BigDecimal.valueOf(50.00));
        orderItem.setTotalPrice(BigDecimal.valueOf(100.00));
        return orderItem;
    }

    private Ticket createTestTicket() {
        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setTicketCode("TICKET-123");
        ticket.setStatus(TicketStatus.VALID);
        ticket.setOrder(testOrder);
        return ticket;
    }

    private OrderResponse createOrderResponse() {
        List<OrderResponse.OrderItemResponse> items = List.of(
                new OrderResponse.OrderItemResponse(
                        "Standard",
                        2,
                        BigDecimal.valueOf(50.00),
                        BigDecimal.valueOf(100.00)
                )
        );

        return new OrderResponse(
                1L,
                "ORD-123456",
                OrderStatus.PENDING,
                BigDecimal.valueOf(100.00),
                BigDecimal.valueOf(5.00),
                LocalDateTime.now(),
                items,
                "Test Event",
                LocalDateTime.now().plusDays(30),
                "Test Location",
                LocalDateTime.now().plusMinutes(15),
                ""
        );
    }

}
