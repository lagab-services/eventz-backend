package com.lagab.eventz.app.domain.payment.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.lagab.eventz.app.domain.order.dto.OrderRequest;
import com.lagab.eventz.app.domain.order.model.Order;
import com.lagab.eventz.app.domain.order.model.OrderItem;
import com.lagab.eventz.app.domain.payment.exception.PaymentException;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Coupon;
import com.stripe.model.checkout.Session;
import com.stripe.param.CouponCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.checkout.SessionRetrieveParams;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class StripeService {

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${stripe.success-url}")
    private String successUrl;
    @Value("${stripe.cancel-url}")
    private String cancelUrl;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
        log.info("Stripe payment provider initialized");
    }

    public Session createCheckoutSession(Order order, OrderRequest request) {
        try {
            log.info("Creating Stripe checkout session for order {}", order.getOrderNumber());

            List<SessionCreateParams.LineItem> lineItems = order.getOrderItems().stream()
                                                                .map(this::createLineItem)
                                                                .toList();

            SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                                                                           .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                                                                           .setMode(SessionCreateParams.Mode.PAYMENT)
                                                                           .setSuccessUrl(getSuccessUrl(request))
                                                                           .setCancelUrl(getCancelUrl(request, order))
                                                                           .addAllLineItem(lineItems)
                                                                           .putMetadata("order_id", order.getId().toString())
                                                                           .putMetadata("user_id", order.getUser().getId().toString())
                                                                           .putMetadata("order_number", order.getOrderNumber())
                                                                           .setExpiresAt(order.getPaymentDeadline()
                                                                                              .toEpochSecond(java.time.ZoneOffset.UTC));

            // Add discount if present
            BigDecimal discount = order.getDiscountAmount();
            if (discount != null && discount.compareTo(BigDecimal.ZERO) > 0) {
                long discountInCents = discount.multiply(BigDecimal.valueOf(100)).longValueExact();
                Coupon coupon = Coupon.create(
                        CouponCreateParams.builder()
                                          .setCurrency("eur")
                                          .setAmountOff(discountInCents)
                                          .setDuration(com.stripe.param.CouponCreateParams.Duration.ONCE)
                                          .build()
                );
                paramsBuilder.addDiscount(
                        SessionCreateParams.Discount.builder()
                                                    .setCoupon(coupon.getId())
                                                    .build()
                );
            }

            // Add customer email if available
            String customerEmail = getCustomerEmail(request, order);
            if (StringUtils.hasText(customerEmail)) {
                paramsBuilder.setCustomerEmail(customerEmail);
            }

            SessionCreateParams params = paramsBuilder.build();
            Session session = Session.create(params);

            log.info("Stripe checkout session created: {} for order {}", session.getId(), order.getOrderNumber());

            return session;

        } catch (StripeException e) {
            log.error("Failed to create Stripe checkout session for order {}", order.getOrderNumber(), e);
            throw new PaymentException("Failed to create checkout session: " + e.getMessage());
        }
    }

    /**
     * Retrieves a Stripe checkout session by its ID
     *
     * @param sessionId The Stripe session ID
     * @return The retrieved Stripe session
     * @throws PaymentException If retrieval fails
     */
    public Session retrieveCheckoutSession(String sessionId) {
        try {
            log.info("Retrieving Stripe checkout session: {}", sessionId);

            Session session = Session.retrieve(sessionId);

            log.info("Successfully retrieved Stripe checkout session: {} with status: {}",
                    sessionId, session.getStatus());

            return session;

        } catch (StripeException e) {
            log.error("Failed to retrieve Stripe checkout session: {}", sessionId, e);
            throw new PaymentException("Failed to retrieve checkout session: " + e.getMessage());
        }
    }

    /**
     * Retrieves a Stripe checkout session with expansion parameters
     *
     * @param sessionId    The Stripe session ID
     * @param expandFields Fields to expand (e.g., "line_items", "customer", "payment_intent")
     * @return The retrieved Stripe session with expanded fields
     * @throws PaymentException If retrieval fails
     */
    public Session retrieveCheckoutSessionWithExpansion(String sessionId, List<String> expandFields) {
        try {
            log.info("Retrieving Stripe checkout session: {} with expansion: {}", sessionId, expandFields);

            SessionRetrieveParams.Builder paramsBuilder = SessionRetrieveParams.builder();

            // Add expansion fields
            if (expandFields != null && !expandFields.isEmpty()) {
                expandFields.forEach(paramsBuilder::addExpand);
            }

            SessionRetrieveParams params = paramsBuilder.build();
            Session session = Session.retrieve(sessionId, params, null);

            log.info("Successfully retrieved Stripe checkout session: {} with status: {} and expanded fields",
                    sessionId, session.getStatus());

            return session;

        } catch (StripeException e) {
            log.error("Failed to retrieve Stripe checkout session with expansion: {}", sessionId, e);
            throw new PaymentException("Failed to retrieve checkout session with expansion: " + e.getMessage());
        }
    }

    /**
     * Retrieves a checkout session with expanded line items
     * Useful for getting complete details of purchased items
     *
     * @param sessionId The Stripe session ID
     * @return The Stripe session with detailed line items
     * @throws PaymentException If retrieval fails
     */
    public Session retrieveCheckoutSessionWithLineItems(String sessionId) {
        return retrieveCheckoutSessionWithExpansion(sessionId, List.of("line_items"));
    }

    /**
     * Retrieves a checkout session with expanded customer and payment_intent
     * Useful for getting complete customer and payment information
     *
     * @param sessionId The Stripe session ID
     * @return The Stripe session with expanded information
     * @throws PaymentException If retrieval fails
     */
    public Session retrieveCheckoutSessionWithCustomerAndPayment(String sessionId) {
        return retrieveCheckoutSessionWithExpansion(sessionId,
                List.of("customer", "payment_intent", "line_items"));
    }

    /**
     * Checks the status of a checkout session
     *
     * @param sessionId The Stripe session ID
     * @return The session status (complete, open, expired)
     * @throws PaymentException If retrieval fails
     */
    public String getCheckoutSessionStatus(String sessionId) {
        Session session = retrieveCheckoutSession(sessionId);
        return session.getStatus();
    }

    /**
     * Checks if a checkout session is completed
     *
     * @param sessionId The Stripe session ID
     * @return true if the session is completed, false otherwise
     * @throws PaymentException If retrieval fails
     */
    public boolean isCheckoutSessionComplete(String sessionId) {
        String status = getCheckoutSessionStatus(sessionId);
        return "complete".equals(status);
    }

    private SessionCreateParams.LineItem createLineItem(OrderItem item) {
        // Convert to cents for Stripe (EUR)
        BigDecimal unitAmountInCents = item.getUnitPrice().multiply(BigDecimal.valueOf(100));

        return SessionCreateParams.LineItem.builder()
                                           .setQuantity((long) item.getQuantity())
                                           .setPriceData(
                                                   SessionCreateParams.LineItem.PriceData.builder()
                                                                                         .setCurrency("eur")
                                                                                         .setUnitAmountDecimal(unitAmountInCents)
                                                                                         .setProductData(
                                                                                                 SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                                                                                                   .setName(
                                                                                                                                                           item.getTicketType()
                                                                                                                                                               .getName())
                                                                                                                                                   .setDescription(
                                                                                                                                                           String.format(
                                                                                                                                                                   "%s - %s",
                                                                                                                                                                   item.getTicketType()
                                                                                                                                                                       .getEvent()
                                                                                                                                                                       .getName(),
                                                                                                                                                                   item.getTicketType()
                                                                                                                                                                       .getDescription()))
                                                                                                                                                   .addImage(
                                                                                                                                                           item.getTicketType()
                                                                                                                                                               .getEvent()
                                                                                                                                                               .getImageUrl())
                                                                                                                                                   .putMetadata(
                                                                                                                                                           "ticket_type_id",
                                                                                                                                                           item.getTicketType()
                                                                                                                                                               .getId()
                                                                                                                                                               .toString())
                                                                                                                                                   .putMetadata(
                                                                                                                                                           "event_id",
                                                                                                                                                           item.getTicketType()
                                                                                                                                                               .getEvent()
                                                                                                                                                               .getId()
                                                                                                                                                               .toString())
                                                                                                                                                   .build())
                                                                                         .build())
                                           .build();
    }

    private String getSuccessUrl(OrderRequest request) {
        if (StringUtils.hasText(request.successUrl())) {
            return request.successUrl();
        }
        return successUrl + "?session_id={CHECKOUT_SESSION_ID}";
    }

    private String getCancelUrl(OrderRequest request, Order order) {
        if (StringUtils.hasText(request.cancelUrl())) {
            return request.cancelUrl();
        }
        return cancelUrl + "?order_id=" + order.getId();
    }

    private String getCustomerEmail(OrderRequest request, Order order) {
        if (StringUtils.hasText(request.billingEmail())) {
            return request.billingEmail();
        }
        return order.getUser().getEmail();
    }
}
