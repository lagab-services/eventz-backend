package com.lagab.eventz.app.domain.payment.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.lagab.eventz.app.domain.event.model.Event;
import com.lagab.eventz.app.domain.event.model.TicketType;
import com.lagab.eventz.app.domain.order.dto.OrderRequest;
import com.lagab.eventz.app.domain.order.model.Order;
import com.lagab.eventz.app.domain.order.model.OrderItem;
import com.lagab.eventz.app.domain.order.model.OrderStatus;
import com.lagab.eventz.app.domain.ticket.dto.AttendeeInfo;
import com.lagab.eventz.app.domain.user.model.User;
import com.stripe.Stripe;
import com.stripe.model.Coupon;
import com.stripe.model.checkout.Session;
import com.stripe.param.CouponCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.checkout.SessionRetrieveParams;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StripeService Tests")
class StripeServiceTest {

    @InjectMocks
    private StripeService stripeService;

    private Order testOrder;
    private OrderRequest testOrderRequest;
    private Event testEvent;
    private TicketType testTicketType;
    private OrderItem testOrderItem;

    @BeforeEach
    void setUp() {
        // Set up configuration values using reflection
        ReflectionTestUtils.setField(stripeService, "stripeSecretKey", "sk_test_123456");
        ReflectionTestUtils.setField(stripeService, "baseUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(stripeService, "successUrl", "http://localhost:8080/success");
        ReflectionTestUtils.setField(stripeService, "cancelUrl", "http://localhost:8080/cancel");

        // Setup test user
        User testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("user@example.com");

        // Setup test event
        testEvent = new Event();
        testEvent.setId(1L);
        testEvent.setName("Test Event");
        testEvent.setImageUrl("http://example.com/image.jpg");

        // Setup test ticket type
        testTicketType = new TicketType();
        testTicketType.setId(1L);
        testTicketType.setName("Standard Ticket");
        testTicketType.setDescription("Standard access ticket");
        testTicketType.setPrice(BigDecimal.valueOf(50.00));
        testTicketType.setEvent(testEvent);

        // Setup test order item
        testOrderItem = new OrderItem();
        testOrderItem.setId(1L);
        testOrderItem.setTicketType(testTicketType);
        testOrderItem.setQuantity(2);
        testOrderItem.setUnitPrice(BigDecimal.valueOf(50.00));
        testOrderItem.setTotalPrice(BigDecimal.valueOf(100.00));

        // Setup test order
        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setOrderNumber("ORD-123456");
        testOrder.setUser(testUser);
        testOrder.setTotalAmount(BigDecimal.valueOf(100.00));
        testOrder.setDiscountAmount(BigDecimal.ZERO);
        testOrder.setStatus(OrderStatus.PENDING);
        testOrder.setPaymentDeadline(LocalDateTime.now().plusMinutes(15));
        testOrder.setOrderItems(List.of(testOrderItem));

        // Setup test order request
        List<AttendeeInfo> attendees = List.of(
                new AttendeeInfo("John", "Doe", "john@example.com", 1L, null)
        );

        testOrderRequest = new OrderRequest(
                "John Doe",                    // billingName
                "john@example.com",           // billingEmail
                "+33123456789",               // billingPhone
                "123 Test Street",            // billingAddress
                "Test City",                  // billingCity
                "12345",                      // billingZipCode
                "France",                     // billingCountry
                attendees,                    // attendees
                "Test notes",                 // notes
                true,                         // acceptTerms
                false,                        // subscribeNewsletter
                "http://custom-success.com",  // successUrl
                "http://custom-cancel.com"    // cancelUrl
        );
    }

    @Nested
    @DisplayName("Initialization Tests")
    class InitializationTests {

        @Test
        @DisplayName("Should initialize Stripe API key on PostConstruct")
        void shouldInitializeStripeApiKeyOnPostConstruct() {
            // When
            stripeService.init();

            // Then
            assertEquals("sk_test_123456", Stripe.apiKey);
        }
    }

    @Nested
    @DisplayName("Create Checkout Session Tests")
    class CreateCheckoutSessionTests {

        @Test
        @DisplayName("Should create checkout session successfully")
        void shouldCreateCheckoutSessionSuccessfully() {
            // Given
            Session mockSession = mock(Session.class);
            when(mockSession.getId()).thenReturn("cs_test_session_id");

            try (MockedStatic<Session> sessionMock = mockStatic(Session.class)) {
                sessionMock.when(() -> Session.create(any(SessionCreateParams.class)))
                           .thenReturn(mockSession);

                // When
                Session result = stripeService.createCheckoutSession(testOrder, testOrderRequest);

                // Then
                assertNotNull(result);
                assertEquals("cs_test_session_id", result.getId());

                sessionMock.verify(() -> Session.create(any(SessionCreateParams.class)));
            }
        }

        @Test
        @DisplayName("Should create checkout session with discount")
        void shouldCreateCheckoutSessionWithDiscount() {
            // Given
            testOrder.setDiscountAmount(BigDecimal.valueOf(10.00));

            Session mockSession = mock(Session.class);
            when(mockSession.getId()).thenReturn("cs_test_session_id");

            Coupon mockCoupon = mock(Coupon.class);
            when(mockCoupon.getId()).thenReturn("coupon_test_id");

            try (MockedStatic<Session> sessionMock = mockStatic(Session.class);
                 MockedStatic<Coupon> couponMock = mockStatic(Coupon.class)) {

                couponMock.when(() -> Coupon.create(any(CouponCreateParams.class)))
                          .thenReturn(mockCoupon);
                sessionMock.when(() -> Session.create(any(SessionCreateParams.class)))
                           .thenReturn(mockSession);

                // When
                Session result = stripeService.createCheckoutSession(testOrder, testOrderRequest);

                // Then
                assertNotNull(result);
                couponMock.verify(() -> Coupon.create(any(CouponCreateParams.class)));
                sessionMock.verify(() -> Session.create(any(SessionCreateParams.class)));
            }
        }

        @Test
        @DisplayName("Should create checkout session without discount when amount is zero")
        void shouldCreateCheckoutSessionWithoutDiscountWhenAmountIsZero() {
            // Given
            testOrder.setDiscountAmount(BigDecimal.ZERO);

            Session mockSession = mock(Session.class);
            when(mockSession.getId()).thenReturn("cs_test_session_id");

            try (MockedStatic<Session> sessionMock = mockStatic(Session.class);
                 MockedStatic<Coupon> couponMock = mockStatic(Coupon.class)) {

                sessionMock.when(() -> Session.create(any(SessionCreateParams.class)))
                           .thenReturn(mockSession);

                // When
                Session result = stripeService.createCheckoutSession(testOrder, testOrderRequest);

                // Then
                assertNotNull(result);
                couponMock.verify(() -> Coupon.create(any(CouponCreateParams.class)), never());
                sessionMock.verify(() -> Session.create(any(SessionCreateParams.class)));
            }
        }

        @Test
        @DisplayName("Should use default URLs when request URLs are empty")
        void shouldUseDefaultUrlsWhenRequestUrlsAreEmpty() {
            // Given
            OrderRequest requestWithoutUrls = new OrderRequest(
                    "John Doe", "john@example.com", "+33123456789",
                    "123 Test Street", "Test City", "12345", "France",
                    List.of(new AttendeeInfo("John", "Doe", "john@example.com", 1L, null)),
                    "Test notes", true, false, "", "" // Empty URLs
            );

            Session mockSession = mock(Session.class);
            when(mockSession.getId()).thenReturn("cs_test_session_id");

            try (MockedStatic<Session> sessionMock = mockStatic(Session.class)) {
                sessionMock.when(() -> Session.create(any(SessionCreateParams.class)))
                           .thenReturn(mockSession);

                // When
                Session result = stripeService.createCheckoutSession(testOrder, requestWithoutUrls);

                // Then
                assertNotNull(result);
                sessionMock.verify(() -> Session.create(any(SessionCreateParams.class)));
            }
        }

    }

    @Nested
    @DisplayName("Retrieve Checkout Session Tests")
    class RetrieveCheckoutSessionTests {

        @Test
        @DisplayName("Should retrieve checkout session successfully")
        void shouldRetrieveCheckoutSessionSuccessfully() {
            // Given
            String sessionId = "cs_test_session_id";
            Session mockSession = mock(Session.class);
            when(mockSession.getId()).thenReturn(sessionId);

            try (MockedStatic<Session> sessionMock = mockStatic(Session.class)) {
                sessionMock.when(() -> Session.retrieve(sessionId))
                           .thenReturn(mockSession);

                // When
                Session result = stripeService.retrieveCheckoutSession(sessionId);

                // Then
                assertNotNull(result);
                assertEquals("cs_test_session_id", result.getId());
                sessionMock.verify(() -> Session.retrieve(sessionId));
            }
        }

    }

    @Nested
    @DisplayName("Retrieve Checkout Session With Expansion Tests")
    class RetrieveCheckoutSessionWithExpansionTests {

        @Test
        @DisplayName("Should retrieve checkout session with expansion successfully")
        void shouldRetrieveCheckoutSessionWithExpansionSuccessfully() {
            // Given
            String sessionId = "cs_test_session_id";
            List<String> expandFields = List.of("line_items", "customer");
            Session mockSession = mock(Session.class);
            when(mockSession.getId()).thenReturn(sessionId);

            try (MockedStatic<Session> sessionMock = mockStatic(Session.class)) {
                sessionMock.when(() -> Session.retrieve(eq(sessionId), any(SessionRetrieveParams.class), isNull()))
                           .thenReturn(mockSession);

                // When
                Session result = stripeService.retrieveCheckoutSessionWithExpansion(sessionId, expandFields);

                // Then
                assertNotNull(result);
                assertEquals("cs_test_session_id", result.getId());
                sessionMock.verify(() -> Session.retrieve(eq(sessionId), any(SessionRetrieveParams.class), isNull()));
            }
        }

    }

    @Nested
    @DisplayName("Convenience Methods Tests")
    class ConvenienceMethodsTests {

        @Test
        @DisplayName("Should retrieve checkout session with line items")
        void shouldRetrieveCheckoutSessionWithLineItems() {
            // Given
            String sessionId = "cs_test_session_id";
            Session mockSession = mock(Session.class);

            try (MockedStatic<Session> sessionMock = mockStatic(Session.class)) {
                sessionMock.when(() -> Session.retrieve(eq(sessionId), any(SessionRetrieveParams.class), isNull()))
                           .thenReturn(mockSession);

                // When
                Session result = stripeService.retrieveCheckoutSessionWithLineItems(sessionId);

                // Then
                assertNotNull(result);
                sessionMock.verify(() -> Session.retrieve(eq(sessionId), any(SessionRetrieveParams.class), isNull()));
            }
        }

        @Test
        @DisplayName("Should retrieve checkout session with customer and payment")
        void shouldRetrieveCheckoutSessionWithCustomerAndPayment() {
            // Given
            String sessionId = "cs_test_session_id";
            Session mockSession = mock(Session.class);

            try (MockedStatic<Session> sessionMock = mockStatic(Session.class)) {
                sessionMock.when(() -> Session.retrieve(eq(sessionId), any(SessionRetrieveParams.class), isNull()))
                           .thenReturn(mockSession);

                // When
                Session result = stripeService.retrieveCheckoutSessionWithCustomerAndPayment(sessionId);

                // Then
                assertNotNull(result);
                sessionMock.verify(() -> Session.retrieve(eq(sessionId), any(SessionRetrieveParams.class), isNull()));
            }
        }

        @Test
        @DisplayName("Should get checkout session status")
        void shouldGetCheckoutSessionStatus() {
            // Given
            String sessionId = "cs_test_session_id";
            Session mockSession = mock(Session.class);
            when(mockSession.getStatus()).thenReturn("complete");

            try (MockedStatic<Session> sessionMock = mockStatic(Session.class)) {
                sessionMock.when(() -> Session.retrieve(sessionId))
                           .thenReturn(mockSession);

                // When
                String status = stripeService.getCheckoutSessionStatus(sessionId);

                // Then
                assertEquals("complete", status);
                sessionMock.verify(() -> Session.retrieve(sessionId));
            }
        }

        @Test
        @DisplayName("Should check if checkout session is complete - true")
        void shouldCheckIfCheckoutSessionIsCompleteTrue() {
            // Given
            String sessionId = "cs_test_session_id";
            Session mockSession = mock(Session.class);
            when(mockSession.getStatus()).thenReturn("complete");

            try (MockedStatic<Session> sessionMock = mockStatic(Session.class)) {
                sessionMock.when(() -> Session.retrieve(sessionId))
                           .thenReturn(mockSession);

                // When
                boolean isComplete = stripeService.isCheckoutSessionComplete(sessionId);

                // Then
                assertTrue(isComplete);
            }
        }

        @Test
        @DisplayName("Should check if checkout session is complete - false")
        void shouldCheckIfCheckoutSessionIsCompleteFalse() {
            // Given
            String sessionId = "cs_test_session_id";
            Session mockSession = mock(Session.class);
            when(mockSession.getStatus()).thenReturn("open");

            try (MockedStatic<Session> sessionMock = mockStatic(Session.class)) {
                sessionMock.when(() -> Session.retrieve(sessionId))
                           .thenReturn(mockSession);

                // When
                boolean isComplete = stripeService.isCheckoutSessionComplete(sessionId);

                // Then
                assertFalse(isComplete);
            }
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle order with null discount amount")
        void shouldHandleOrderWithNullDiscountAmount() {
            // Given
            testOrder.setDiscountAmount(null);
            Session mockSession = mock(Session.class);
            when(mockSession.getId()).thenReturn("cs_test_session_id");

            try (MockedStatic<Session> sessionMock = mockStatic(Session.class)) {
                sessionMock.when(() -> Session.create(any(SessionCreateParams.class)))
                           .thenReturn(mockSession);

                // When
                Session result = stripeService.createCheckoutSession(testOrder, testOrderRequest);

                // Then
                assertNotNull(result);
                sessionMock.verify(() -> Session.create(any(SessionCreateParams.class)));
            }
        }

        @Test
        @DisplayName("Should handle order with negative discount amount")
        void shouldHandleOrderWithNegativeDiscountAmount() {
            // Given
            testOrder.setDiscountAmount(BigDecimal.valueOf(-5.00));
            Session mockSession = mock(Session.class);
            when(mockSession.getId()).thenReturn("cs_test_session_id");

            try (MockedStatic<Session> sessionMock = mockStatic(Session.class)) {
                sessionMock.when(() -> Session.create(any(SessionCreateParams.class)))
                           .thenReturn(mockSession);

                // When
                Session result = stripeService.createCheckoutSession(testOrder, testOrderRequest);

                // Then
                assertNotNull(result);
                // Should not create coupon for negative discount
                sessionMock.verify(() -> Session.create(any(SessionCreateParams.class)));
            }
        }

        @Test
        @DisplayName("Should handle order with empty order items list")
        void shouldHandleOrderWithEmptyOrderItemsList() {
            // Given
            testOrder.setOrderItems(List.of());
            Session mockSession = mock(Session.class);
            when(mockSession.getId()).thenReturn("cs_test_session_id");

            try (MockedStatic<Session> sessionMock = mockStatic(Session.class)) {
                sessionMock.when(() -> Session.create(any(SessionCreateParams.class)))
                           .thenReturn(mockSession);

                // When
                Session result = stripeService.createCheckoutSession(testOrder, testOrderRequest);

                // Then
                assertNotNull(result);
                sessionMock.verify(() -> Session.create(any(SessionCreateParams.class)));
            }
        }

        @Test
        @DisplayName("Should handle order item with zero price")
        void shouldHandleOrderItemWithZeroPrice() {
            // Given
            testOrderItem.setUnitPrice(BigDecimal.ZERO);
            testOrderItem.setTotalPrice(BigDecimal.ZERO);
            Session mockSession = mock(Session.class);
            when(mockSession.getId()).thenReturn("cs_test_session_id");

            try (MockedStatic<Session> sessionMock = mockStatic(Session.class)) {
                sessionMock.when(() -> Session.create(any(SessionCreateParams.class)))
                           .thenReturn(mockSession);

                // When
                Session result = stripeService.createCheckoutSession(testOrder, testOrderRequest);

                // Then
                assertNotNull(result);
                sessionMock.verify(() -> Session.create(any(SessionCreateParams.class)));
            }
        }

        @Test
        @DisplayName("Should handle ticket type with null description")
        void shouldHandleTicketTypeWithNullDescription() {
            // Given
            testTicketType.setDescription(null);
            Session mockSession = mock(Session.class);
            when(mockSession.getId()).thenReturn("cs_test_session_id");

            try (MockedStatic<Session> sessionMock = mockStatic(Session.class)) {
                sessionMock.when(() -> Session.create(any(SessionCreateParams.class)))
                           .thenReturn(mockSession);

                // When
                Session result = stripeService.createCheckoutSession(testOrder, testOrderRequest);

                // Then
                assertNotNull(result);
                sessionMock.verify(() -> Session.create(any(SessionCreateParams.class)));
            }
        }

        @Test
        @DisplayName("Should use user email when billing email is empty")
        void shouldUseUserEmailWhenBillingEmailIsEmpty() {
            // Given
            OrderRequest requestWithEmptyEmail = new OrderRequest(
                    "John Doe", "", "+33123456789", // Empty billing email
                    "123 Test Street", "Test City", "12345", "France",
                    List.of(new AttendeeInfo("John", "Doe", "john@example.com", 1L, null)),
                    "Test notes", true, false,
                    "http://custom-success.com", "http://custom-cancel.com"
            );

            Session mockSession = mock(Session.class);
            when(mockSession.getId()).thenReturn("cs_test_session_id");

            try (MockedStatic<Session> sessionMock = mockStatic(Session.class)) {
                sessionMock.when(() -> Session.create(any(SessionCreateParams.class)))
                           .thenReturn(mockSession);

                // When
                Session result = stripeService.createCheckoutSession(testOrder, requestWithEmptyEmail);

                // Then
                assertNotNull(result);
                sessionMock.verify(() -> Session.create(any(SessionCreateParams.class)));
            }
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should handle complete checkout flow")
        void shouldHandleCompleteCheckoutFlow() {
            // Given
            String sessionId = "cs_test_session_id";
            Session mockSession = mock(Session.class);
            when(mockSession.getId()).thenReturn(sessionId);
            when(mockSession.getStatus()).thenReturn("complete");

            try (MockedStatic<Session> sessionMock = mockStatic(Session.class)) {
                // Mock session creation
                sessionMock.when(() -> Session.create(any(SessionCreateParams.class)))
                           .thenReturn(mockSession);

                // Mock session retrieval
                sessionMock.when(() -> Session.retrieve(sessionId))
                           .thenReturn(mockSession);

                // When - Create session
                Session createdSession = stripeService.createCheckoutSession(testOrder, testOrderRequest);

                // When - Check status
                String status = stripeService.getCheckoutSessionStatus(sessionId);
                boolean isComplete = stripeService.isCheckoutSessionComplete(sessionId);

                // Then
                assertNotNull(createdSession);
                assertEquals("complete", status);
                assertTrue(isComplete);

                sessionMock.verify(() -> Session.create(any(SessionCreateParams.class)));
                sessionMock.verify(() -> Session.retrieve(sessionId), times(2)); // Called twice
            }
        }

        @Test
        @DisplayName("Should handle order with discount and multiple items")
        void shouldHandleOrderWithDiscountAndMultipleItems() {
            // Given
            TicketType premiumTicketType = new TicketType();
            premiumTicketType.setId(3L);
            premiumTicketType.setName("Premium Ticket");
            premiumTicketType.setDescription("Premium access");
            premiumTicketType.setPrice(BigDecimal.valueOf(75.00));
            premiumTicketType.setEvent(testEvent);

            OrderItem premiumOrderItem = new OrderItem();
            premiumOrderItem.setId(3L);
            premiumOrderItem.setTicketType(premiumTicketType);
            premiumOrderItem.setQuantity(1);
            premiumOrderItem.setUnitPrice(BigDecimal.valueOf(75.00));
            premiumOrderItem.setTotalPrice(BigDecimal.valueOf(75.00));

            testOrder.setOrderItems(List.of(testOrderItem, premiumOrderItem));
            testOrder.setDiscountAmount(BigDecimal.valueOf(15.00));

            Session mockSession = mock(Session.class);
            when(mockSession.getId()).thenReturn("cs_test_session_id");

            Coupon mockCoupon = mock(Coupon.class);
            when(mockCoupon.getId()).thenReturn("coupon_test_id");

            try (MockedStatic<Session> sessionMock = mockStatic(Session.class);
                 MockedStatic<Coupon> couponMock = mockStatic(Coupon.class)) {

                couponMock.when(() -> Coupon.create(any(CouponCreateParams.class)))
                          .thenReturn(mockCoupon);
                sessionMock.when(() -> Session.create(any(SessionCreateParams.class)))
                           .thenReturn(mockSession);

                // When
                Session result = stripeService.createCheckoutSession(testOrder, testOrderRequest);

                // Then
                assertNotNull(result);
                couponMock.verify(() -> Coupon.create(any(CouponCreateParams.class)));
                sessionMock.verify(() -> Session.create(any(SessionCreateParams.class)));
            }
        }
    }
}
