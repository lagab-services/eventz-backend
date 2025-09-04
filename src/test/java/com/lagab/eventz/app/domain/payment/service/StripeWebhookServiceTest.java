package com.lagab.eventz.app.domain.payment.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.lagab.eventz.app.domain.order.model.Order;
import com.lagab.eventz.app.domain.order.service.OrderService;
import com.lagab.eventz.app.domain.payment.exception.PaymentException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StripeWebhookService Tests")
class StripeWebhookServiceTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private StripeWebhookService stripeWebhookService;

    private String testPayload;
    private String testSigHeader;
    private String webhookSecret;
    private Event mockEvent;
    private Session mockSession;
    private EventDataObjectDeserializer mockDeserializer;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        // Setup test data
        testPayload = "{\"id\":\"evt_test_webhook\",\"object\":\"event\"}";
        testSigHeader = "t=1234567890,v1=test_signature";
        webhookSecret = "whsec_test_secret";

        // Set the webhook secret using reflection
        ReflectionTestUtils.setField(stripeWebhookService, "webhookSecret", webhookSecret);

        // Setup mock event
        mockEvent = mock(Event.class);
        mockDeserializer = mock(EventDataObjectDeserializer.class);

        // Setup test order
        testOrder = new Order();
        testOrder.setId(123L);
        testOrder.setOrderNumber("ORD-123456");
    }

    @Nested
    @DisplayName("Handle Webhook Tests")
    class HandleWebhookTests {

        @Test
        @DisplayName("Should handle checkout.session.completed successfully")
        void shouldHandleCheckoutSessionCompletedSuccessfully() {
            // Given
            mockSession = mock(Session.class);
            Map<String, String> metadata = new HashMap<>();
            metadata.put("order_id", "123");
            when(mockSession.getMetadata()).thenReturn(metadata);
            try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
                webhookMock.when(() -> Webhook.constructEvent(testPayload, testSigHeader, webhookSecret))
                           .thenReturn(mockEvent);

                when(mockEvent.getType()).thenReturn("checkout.session.completed");
                when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
                when(mockDeserializer.getObject()).thenReturn(Optional.of(mockSession));
                when(orderService.finalizeOrder(123L, mockSession)).thenReturn(testOrder);

                // When
                stripeWebhookService.handleWebhook(testPayload, testSigHeader);

                // Then
                verify(orderService).finalizeOrder(123L, mockSession);
                verify(orderService, never()).expireOrder(anyLong());
                verify(orderService, never()).abortOrder(anyLong());
            }
        }

        @Test
        @DisplayName("Should handle checkout.session.expired successfully")
        void shouldHandleCheckoutSessionExpiredSuccessfully() {
            // Given
            mockSession = mock(Session.class);
            Map<String, String> metadata = new HashMap<>();
            metadata.put("order_id", "123");
            when(mockSession.getMetadata()).thenReturn(metadata);
            try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
                webhookMock.when(() -> Webhook.constructEvent(testPayload, testSigHeader, webhookSecret))
                           .thenReturn(mockEvent);

                when(mockEvent.getType()).thenReturn("checkout.session.expired");
                when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
                when(mockDeserializer.getObject()).thenReturn(Optional.of(mockSession));

                // When
                stripeWebhookService.handleWebhook(testPayload, testSigHeader);

                // Then
                verify(orderService).expireOrder(123L);
                verify(orderService, never()).finalizeOrder(anyLong(), any());
                verify(orderService, never()).abortOrder(anyLong());
            }
        }

        @Test
        @DisplayName("Should handle payment_intent.succeeded")
        void shouldHandlePaymentIntentSucceeded() {
            // Given
            try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
                webhookMock.when(() -> Webhook.constructEvent(testPayload, testSigHeader, webhookSecret))
                           .thenReturn(mockEvent);

                when(mockEvent.getType()).thenReturn("payment_intent.succeeded");

                // When
                stripeWebhookService.handleWebhook(testPayload, testSigHeader);

                // Then
                verify(orderService, never()).finalizeOrder(anyLong(), any());
                verify(orderService, never()).expireOrder(anyLong());
                verify(orderService, never()).abortOrder(anyLong());
            }
        }

        @Test
        @DisplayName("Should handle payment_intent.payment_failed")
        void shouldHandlePaymentIntentPaymentFailed() {
            // Given

            try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
                webhookMock.when(() -> Webhook.constructEvent(testPayload, testSigHeader, webhookSecret))
                           .thenReturn(mockEvent);

                when(mockEvent.getType()).thenReturn("payment_intent.payment_failed");
                when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
                PaymentIntent mockPaymentIntent;
                // Setup mock PaymentIntent
                mockPaymentIntent = mock(PaymentIntent.class);
                Map<String, String> paymentIntentMetadata = new HashMap<>();
                paymentIntentMetadata.put("order_id", "123");
                when(mockPaymentIntent.getMetadata()).thenReturn(paymentIntentMetadata);

                when(mockDeserializer.getObject()).thenReturn(Optional.of(mockPaymentIntent));

                // When
                stripeWebhookService.handleWebhook(testPayload, testSigHeader);

                // Then
                verify(orderService).abortOrder(123L);
                verify(orderService, never()).finalizeOrder(anyLong(), any());
                verify(orderService, never()).expireOrder(anyLong());
            }
        }

        @Test
        @DisplayName("Should handle unhandled event types gracefully")
        void shouldHandleUnhandledEventTypesGracefully() {
            // Given
            try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
                webhookMock.when(() -> Webhook.constructEvent(testPayload, testSigHeader, webhookSecret))
                           .thenReturn(mockEvent);

                when(mockEvent.getType()).thenReturn("invoice.payment_succeeded");

                // When
                stripeWebhookService.handleWebhook(testPayload, testSigHeader);

                // Then
                verify(orderService, never()).finalizeOrder(anyLong(), any());
                verify(orderService, never()).expireOrder(anyLong());
                verify(orderService, never()).abortOrder(anyLong());
            }
        }

        @Test
        @DisplayName("Should throw PaymentException when signature verification fails")
        void shouldThrowPaymentExceptionWhenSignatureVerificationFails() {
            // Given
            try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
                webhookMock.when(() -> Webhook.constructEvent(testPayload, testSigHeader, webhookSecret))
                           .thenThrow(new SignatureVerificationException("Invalid signature", testSigHeader));

                // When & Then
                PaymentException exception = assertThrows(PaymentException.class,
                        () -> stripeWebhookService.handleWebhook(testPayload, testSigHeader));

                assertEquals("Invalid webhook signature", exception.getMessage());
                verify(orderService, never()).finalizeOrder(anyLong(), any());
                verify(orderService, never()).expireOrder(anyLong());
                verify(orderService, never()).abortOrder(anyLong());
            }
        }
    }

    @Nested
    @DisplayName("Checkout Session Completed Tests")
    class CheckoutSessionCompletedTests {

        @Test
        @DisplayName("Should process checkout session completed with valid order ID")
        void shouldProcessCheckoutSessionCompletedWithValidOrderId() {
            // Given
            mockSession = mock(Session.class);
            Map<String, String> metadata = new HashMap<>();
            metadata.put("order_id", "123");
            when(mockSession.getMetadata()).thenReturn(metadata);
            try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
                webhookMock.when(() -> Webhook.constructEvent(testPayload, testSigHeader, webhookSecret))
                           .thenReturn(mockEvent);

                when(mockEvent.getType()).thenReturn("checkout.session.completed");
                when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
                when(mockDeserializer.getObject()).thenReturn(Optional.of(mockSession));
                when(orderService.finalizeOrder(123L, mockSession)).thenReturn(testOrder);

                // When
                stripeWebhookService.handleWebhook(testPayload, testSigHeader);

                // Then
                verify(orderService).finalizeOrder(123L, mockSession);
            }
        }

        @Test
        @DisplayName("Should throw PaymentException when session object is missing")
        void shouldThrowPaymentExceptionWhenSessionObjectIsMissing() {
            // Given
            try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
                webhookMock.when(() -> Webhook.constructEvent(testPayload, testSigHeader, webhookSecret))
                           .thenReturn(mockEvent);

                when(mockEvent.getType()).thenReturn("checkout.session.completed");
                when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
                when(mockDeserializer.getObject()).thenReturn(Optional.empty());

                // When & Then
                PaymentException exception = assertThrows(PaymentException.class,
                        () -> stripeWebhookService.handleWebhook(testPayload, testSigHeader));

                assertEquals("Failed to process completed checkout session", exception.getMessage());
                verify(orderService, never()).finalizeOrder(anyLong(), any());
            }
        }

        @Test
        @DisplayName("Should throw PaymentException when order ID is missing from metadata")
        void shouldThrowPaymentExceptionWhenOrderIdIsMissingFromMetadata() {
            // Given
            Map<String, String> emptyMetadata = new HashMap<>();
            // Setup mock session
            mockSession = mock(Session.class);
            Map<String, String> metadata = new HashMap<>();
            when(mockSession.getMetadata()).thenReturn(metadata);

            when(mockSession.getMetadata()).thenReturn(emptyMetadata);

            try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
                webhookMock.when(() -> Webhook.constructEvent(testPayload, testSigHeader, webhookSecret))
                           .thenReturn(mockEvent);

                when(mockEvent.getType()).thenReturn("checkout.session.completed");
                when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
                when(mockDeserializer.getObject()).thenReturn(Optional.of(mockSession));

                // When & Then
                PaymentException exception = assertThrows(PaymentException.class,
                        () -> stripeWebhookService.handleWebhook(testPayload, testSigHeader));

                assertEquals("Failed to process completed checkout session", exception.getMessage());
                verify(orderService, never()).finalizeOrder(anyLong(), any());
            }
        }

        @Test
        @DisplayName("Should throw PaymentException when order service throws exception")
        void shouldThrowPaymentExceptionWhenOrderServiceThrowsException() {
            // Given
            mockSession = mock(Session.class);
            Map<String, String> metadata = new HashMap<>();
            metadata.put("order_id", "123");
            when(mockSession.getMetadata()).thenReturn(metadata);
            try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
                webhookMock.when(() -> Webhook.constructEvent(testPayload, testSigHeader, webhookSecret))
                           .thenReturn(mockEvent);

                when(mockEvent.getType()).thenReturn("checkout.session.completed");
                when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
                when(mockDeserializer.getObject()).thenReturn(Optional.of(mockSession));
                when(orderService.finalizeOrder(123L, mockSession))
                        .thenThrow(new RuntimeException("Order processing failed"));

                // When & Then
                PaymentException exception = assertThrows(PaymentException.class,
                        () -> stripeWebhookService.handleWebhook(testPayload, testSigHeader));

                assertEquals("Failed to process completed checkout session", exception.getMessage());
                verify(orderService).finalizeOrder(123L, mockSession);
            }
        }
    }

    @Nested
    @DisplayName("Checkout Session Expired Tests")
    class CheckoutSessionExpiredTests {

        @Test
        @DisplayName("Should process checkout session expired gracefully")
        void shouldProcessCheckoutSessionExpiredGracefully() {
            // Given
            mockSession = mock(Session.class);
            Map<String, String> metadata = new HashMap<>();
            metadata.put("order_id", "123");
            when(mockSession.getMetadata()).thenReturn(metadata);
            try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
                webhookMock.when(() -> Webhook.constructEvent(testPayload, testSigHeader, webhookSecret))
                           .thenReturn(mockEvent);

                when(mockEvent.getType()).thenReturn("checkout.session.expired");
                when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
                when(mockDeserializer.getObject()).thenReturn(Optional.of(mockSession));

                // When
                stripeWebhookService.handleWebhook(testPayload, testSigHeader);

                // Then
                verify(orderService).expireOrder(123L);
            }
        }

        @Test
        @DisplayName("Should not fail webhook when expiration processing fails")
        void shouldNotFailWebhookWhenExpirationProcessingFails() {
            // Given
            mockSession = mock(Session.class);
            Map<String, String> metadata = new HashMap<>();
            metadata.put("order_id", "123");
            when(mockSession.getMetadata()).thenReturn(metadata);
            try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
                webhookMock.when(() -> Webhook.constructEvent(testPayload, testSigHeader, webhookSecret))
                           .thenReturn(mockEvent);

                when(mockEvent.getType()).thenReturn("checkout.session.expired");
                when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
                when(mockDeserializer.getObject()).thenReturn(Optional.of(mockSession));
                doThrow(new RuntimeException("Order expiration failed"))
                        .when(orderService).expireOrder(123L);

                // When & Then - Should not throw exception
                assertDoesNotThrow(() -> stripeWebhookService.handleWebhook(testPayload, testSigHeader));

                verify(orderService).expireOrder(123L);
            }
        }

        @Test
        @DisplayName("Should handle expired session with missing session object gracefully")
        void shouldHandleExpiredSessionWithMissingSessionObjectGracefully() {
            // Given
            try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
                webhookMock.when(() -> Webhook.constructEvent(testPayload, testSigHeader, webhookSecret))
                           .thenReturn(mockEvent);

                when(mockEvent.getType()).thenReturn("checkout.session.expired");
                when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
                when(mockDeserializer.getObject()).thenReturn(Optional.empty());

                // When & Then - Should not throw exception for expired sessions
                assertDoesNotThrow(() -> stripeWebhookService.handleWebhook(testPayload, testSigHeader));

                verify(orderService, never()).expireOrder(anyLong());
            }
        }
    }

    @Nested
    @DisplayName("Payment Intent Failed Tests")
    class PaymentIntentFailedTests {

        private PaymentIntent mockPaymentIntent;

        @BeforeEach
        void setUpPaymentIntent() {
            // Setup mock PaymentIntent
            mockPaymentIntent = mock(PaymentIntent.class);
            Map<String, String> paymentIntentMetadata = new HashMap<>();
            paymentIntentMetadata.put("order_id", "123");
            lenient().when(mockPaymentIntent.getMetadata()).thenReturn(paymentIntentMetadata);
        }

        @Test
        @DisplayName("Should abort order when payment intent fails")
        void shouldAbortOrderWhenPaymentIntentFails() {
            // Given
            try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
                webhookMock.when(() -> Webhook.constructEvent(testPayload, testSigHeader, webhookSecret))
                           .thenReturn(mockEvent);

                when(mockEvent.getType()).thenReturn("payment_intent.payment_failed");
                when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
                when(mockDeserializer.getObject()).thenReturn(Optional.of(mockPaymentIntent));

                // When
                stripeWebhookService.handleWebhook(testPayload, testSigHeader);

                // Then
                verify(orderService).abortOrder(123L);
            }
        }

        @Test
        @DisplayName("Should handle payment intent failed with missing PaymentIntent object")
        void shouldHandlePaymentIntentFailedWithMissingPaymentIntentObject() {
            // Given
            try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
                webhookMock.when(() -> Webhook.constructEvent(testPayload, testSigHeader, webhookSecret))
                           .thenReturn(mockEvent);

                when(mockEvent.getType()).thenReturn("payment_intent.payment_failed");
                when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
                when(mockDeserializer.getObject()).thenReturn(Optional.empty());

                // When & Then - Should not throw exception due to try-catch in service
                assertDoesNotThrow(() -> stripeWebhookService.handleWebhook(testPayload, testSigHeader));

                verify(orderService, never()).abortOrder(anyLong());
            }
        }

        @Test
        @DisplayName("Should handle payment intent failed with invalid order ID")
        void shouldHandlePaymentIntentFailedWithInvalidOrderId() {
            // Given
            Map<String, String> invalidMetadata = new HashMap<>();
            invalidMetadata.put("order_id", "not_a_number");
            when(mockPaymentIntent.getMetadata()).thenReturn(invalidMetadata);

            try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
                webhookMock.when(() -> Webhook.constructEvent(testPayload, testSigHeader, webhookSecret))
                           .thenReturn(mockEvent);

                when(mockEvent.getType()).thenReturn("payment_intent.payment_failed");
                when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
                when(mockDeserializer.getObject()).thenReturn(Optional.of(mockPaymentIntent));

                // When & Then - Should not throw exception due to try-catch in service
                assertDoesNotThrow(() -> stripeWebhookService.handleWebhook(testPayload, testSigHeader));

                verify(orderService, never()).abortOrder(anyLong());
            }
        }

        @Test
        @DisplayName("Should handle payment intent failed with missing order ID")
        void shouldHandlePaymentIntentFailedWithMissingOrderId() {
            // Given
            Map<String, String> emptyMetadata = new HashMap<>();
            when(mockPaymentIntent.getMetadata()).thenReturn(emptyMetadata);

            try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
                webhookMock.when(() -> Webhook.constructEvent(testPayload, testSigHeader, webhookSecret))
                           .thenReturn(mockEvent);

                when(mockEvent.getType()).thenReturn("payment_intent.payment_failed");
                when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
                when(mockDeserializer.getObject()).thenReturn(Optional.of(mockPaymentIntent));

                // When & Then - Should not throw exception due to try-catch in service
                assertDoesNotThrow(() -> stripeWebhookService.handleWebhook(testPayload, testSigHeader));

                verify(orderService, never()).abortOrder(anyLong());
            }
        }

        @Test
        @DisplayName("Should handle payment intent failed with null metadata")
        void shouldHandlePaymentIntentFailedWithNullMetadata() {
            // Given
            when(mockPaymentIntent.getMetadata()).thenReturn(null);

            try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
                webhookMock.when(() -> Webhook.constructEvent(testPayload, testSigHeader, webhookSecret))
                           .thenReturn(mockEvent);

                when(mockEvent.getType()).thenReturn("payment_intent.payment_failed");
                when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
                when(mockDeserializer.getObject()).thenReturn(Optional.of(mockPaymentIntent));

                // When & Then - Should not throw exception due to try-catch in service
                assertDoesNotThrow(() -> stripeWebhookService.handleWebhook(testPayload, testSigHeader));

                verify(orderService, never()).abortOrder(anyLong());
            }
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle null payload")
        void shouldHandleNullPayload() {
            // Given
            try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
                webhookMock.when(() -> Webhook.constructEvent(null, testSigHeader, webhookSecret))
                           .thenThrow(new SignatureVerificationException("Null payload", testSigHeader));

                // When & Then
                PaymentException exception = assertThrows(PaymentException.class,
                        () -> stripeWebhookService.handleWebhook(null, testSigHeader));

                assertEquals("Invalid webhook signature", exception.getMessage());
            }
        }

        @Test
        @DisplayName("Should handle empty webhook secret")
        void shouldHandleEmptyWebhookSecret() {
            // Given
            ReflectionTestUtils.setField(stripeWebhookService, "webhookSecret", "");

            try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
                webhookMock.when(() -> Webhook.constructEvent(testPayload, testSigHeader, ""))
                           .thenThrow(new SignatureVerificationException("Empty secret", testSigHeader));

                // When & Then
                PaymentException exception = assertThrows(PaymentException.class,
                        () -> stripeWebhookService.handleWebhook(testPayload, testSigHeader));

                assertEquals("Invalid webhook signature", exception.getMessage());
            }
        }

        @Test
        @DisplayName("Should handle malformed event data")
        void shouldHandleMalformedEventData() {
            // Given
            try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
                webhookMock.when(() -> Webhook.constructEvent(testPayload, testSigHeader, webhookSecret))
                           .thenReturn(mockEvent);

                when(mockEvent.getType()).thenReturn("checkout.session.completed");
                when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
                when(mockDeserializer.getObject()).thenThrow(new RuntimeException("Malformed data"));

                // When & Then
                PaymentException exception = assertThrows(PaymentException.class,
                        () -> stripeWebhookService.handleWebhook(testPayload, testSigHeader));

                assertEquals("Failed to process completed checkout session", exception.getMessage());
            }
        }

        @Test
        @DisplayName("Should handle session with null metadata")
        void shouldHandleSessionWithNullMetadata() {
            // Given
            mockSession = mock(Session.class);
            when(mockSession.getMetadata()).thenReturn(null);

            try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
                webhookMock.when(() -> Webhook.constructEvent(testPayload, testSigHeader, webhookSecret))
                           .thenReturn(mockEvent);

                when(mockEvent.getType()).thenReturn("checkout.session.completed");
                when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
                when(mockDeserializer.getObject()).thenReturn(Optional.of(mockSession));

                // When & Then
                PaymentException exception = assertThrows(PaymentException.class,
                        () -> stripeWebhookService.handleWebhook(testPayload, testSigHeader));

                assertEquals("Failed to process completed checkout session", exception.getMessage());
                verify(orderService, never()).finalizeOrder(anyLong(), any());
            }
        }

        @Test
        @DisplayName("Should handle multiple event types in sequence")
        void shouldHandleMultipleEventTypesInSequence() {
            // Given
            mockSession = mock(Session.class);
            Map<String, String> metadata = new HashMap<>();
            metadata.put("order_id", "123");
            when(mockSession.getMetadata()).thenReturn(metadata);
            try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
                // First event - checkout.session.completed
                webhookMock.when(() -> Webhook.constructEvent(testPayload, testSigHeader, webhookSecret))
                           .thenReturn(mockEvent);

                when(mockEvent.getType()).thenReturn("checkout.session.completed");
                when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
                when(mockDeserializer.getObject()).thenReturn(Optional.of(mockSession));
                when(orderService.finalizeOrder(123L, mockSession)).thenReturn(testOrder);

                // When - First webhook
                stripeWebhookService.handleWebhook(testPayload, testSigHeader);

                // Then - Verify first event processed
                verify(orderService).finalizeOrder(123L, mockSession);

                // Given - Second event - payment_intent.succeeded
                when(mockEvent.getType()).thenReturn("payment_intent.succeeded");

                // When - Second webhook
                stripeWebhookService.handleWebhook(testPayload, testSigHeader);

                // Then - Verify no additional order service calls
                verify(orderService, times(1)).finalizeOrder(anyLong(), any());
                verify(orderService, never()).expireOrder(anyLong());
                verify(orderService, never()).abortOrder(anyLong());
            }
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should handle complete payment flow")
        void shouldHandleCompletePaymentFlow() {
            // Given
            mockSession = mock(Session.class);
            Map<String, String> metadata = new HashMap<>();
            metadata.put("order_id", "123");
            when(mockSession.getMetadata()).thenReturn(metadata);
            try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
                webhookMock.when(() -> Webhook.constructEvent(testPayload, testSigHeader, webhookSecret))
                           .thenReturn(mockEvent);

                // Simulate checkout.session.completed
                when(mockEvent.getType()).thenReturn("checkout.session.completed");
                when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
                when(mockDeserializer.getObject()).thenReturn(Optional.of(mockSession));
                when(orderService.finalizeOrder(123L, mockSession)).thenReturn(testOrder);

                // When
                stripeWebhookService.handleWebhook(testPayload, testSigHeader);

                // Then
                verify(orderService).finalizeOrder(123L, mockSession);

                // Simulate payment_intent.succeeded (additional confirmation)
                when(mockEvent.getType()).thenReturn("payment_intent.succeeded");

                // When
                stripeWebhookService.handleWebhook(testPayload, testSigHeader);

                // Then - Should not cause additional processing
                verify(orderService, times(1)).finalizeOrder(anyLong(), any());
            }
        }

        @Test
        @DisplayName("Should handle payment failure flow")
        void shouldHandlePaymentFailureFlow() {
            // Given
            PaymentIntent mockPaymentIntent = mock(PaymentIntent.class);
            Map<String, String> paymentIntentMetadata = new HashMap<>();
            paymentIntentMetadata.put("order_id", "123");
            when(mockPaymentIntent.getMetadata()).thenReturn(paymentIntentMetadata);

            try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
                webhookMock.when(() -> Webhook.constructEvent(testPayload, testSigHeader, webhookSecret))
                           .thenReturn(mockEvent);

                // Simulate payment_intent.payment_failed
                when(mockEvent.getType()).thenReturn("payment_intent.payment_failed");
                when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
                when(mockDeserializer.getObject()).thenReturn(Optional.of(mockPaymentIntent));

                // When
                stripeWebhookService.handleWebhook(testPayload, testSigHeader);

                // Then
                verify(orderService).abortOrder(123L);
                verify(orderService, never()).finalizeOrder(anyLong(), any());
                verify(orderService, never()).expireOrder(anyLong());
            }
        }

    }
}
