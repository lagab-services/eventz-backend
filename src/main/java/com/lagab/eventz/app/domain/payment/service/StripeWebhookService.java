package com.lagab.eventz.app.domain.payment.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lagab.eventz.app.domain.order.model.Order;
import com.lagab.eventz.app.domain.order.service.OrderService;
import com.lagab.eventz.app.domain.payment.exception.PaymentException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookService {

    public static final String ORDER_ID = "order_id";
    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    private final OrderService orderService;

    public void handleWebhook(String payload, String sigHeader) {
        Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.error("Invalid webhook signature", e);
            throw new PaymentException("Invalid webhook signature");
        }

        log.info("Processing webhook event: {}", event.getType());

        switch (event.getType()) {
        case "checkout.session.completed":
            handleCheckoutSessionCompleted(event);
            break;
        case "checkout.session.expired":
            handleCheckoutSessionExpired(event);
            break;
        case "payment_intent.succeeded":
            handlePaymentIntentSucceeded(event);
            break;
        case "payment_intent.payment_failed":
            handlePaymentIntentFailed(event);
            break;
        default:
            log.info("Unhandled webhook event type: {}", event.getType());
        }
    }

    private void handleCheckoutSessionCompleted(Event event) {
        try {
            Session session = (Session) event.getDataObjectDeserializer()
                                             .getObject().orElseThrow();

            String orderIdStr = session.getMetadata().get(ORDER_ID);
            if (orderIdStr == null || orderIdStr.trim().isEmpty()) {
                throw new PaymentException("Order ID missing from session metadata");
            }

            Long orderId = Long.valueOf(orderIdStr);
            log.info("Processing completed checkout session {} for order {}", session.getId(), orderId);

            // Finalize the order
            Order order = orderService.finalizeOrder(orderId, session);

            log.info("Order {} successfully processed and tickets generated", order.getOrderNumber());

        } catch (Exception e) {
            log.error("Error processing checkout.session.completed webhook", e);
            throw new PaymentException("Failed to process completed checkout session");
        }
    }

    private void handleCheckoutSessionExpired(Event event) {
        try {
            Session session = (Session) event.getDataObjectDeserializer()
                                             .getObject().orElseThrow();

            String orderIdStr = session.getMetadata().get(ORDER_ID);
            if (orderIdStr == null || orderIdStr.trim().isEmpty()) {
                throw new PaymentException("Order ID missing from session metadata");
            }

            Long orderId = Long.valueOf(orderIdStr);
            log.info("Processing expired checkout session {} for order {}", session.getId(), orderId);

            orderService.expireOrder(orderId);

        } catch (Exception e) {
            log.error("Error processing checkout.session.expired webhook", e);
            // Don't fail the webhook for expirations
        }
    }

    private void handlePaymentIntentSucceeded(Event event) {
        log.info("Payment intent succeeded - additional confirmation " + event.getId());
        // Additional processing if needed
    }

    private void handlePaymentIntentFailed(Event event) {
        log.warn("Payment intent failed");
        try {
            PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                                                               .getObject().orElseThrow();

            // Récupérer l'order ID depuis les métadonnées du PaymentIntent
            Long orderId = Long.valueOf(paymentIntent.getMetadata().get(ORDER_ID));
            orderService.abortOrder(orderId);
        } catch (Exception e) {
            log.error("Error processing payment_intent.payment_failed webhook", e);
            // Optionnel : relancer l'exception ou la gérer silencieusement
        }
    }
}
