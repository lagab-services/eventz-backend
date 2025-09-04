package com.lagab.eventz.app.interfaces.web.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lagab.eventz.app.domain.order.dto.CheckoutResponse;
import com.lagab.eventz.app.domain.order.dto.OrderRequest;
import com.lagab.eventz.app.domain.order.dto.OrderResponse;
import com.lagab.eventz.app.domain.order.service.OrderService;
import com.lagab.eventz.app.util.SecurityUtils;
import com.stripe.model.checkout.Session;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Orders", description = "Order management")
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout")
    @Operation(summary = "Create checkout session", description = "Create Stripe checkout session from cart")
    public ResponseEntity<CheckoutResponse> createCheckoutSession(
            HttpServletRequest request,
            @Valid @RequestBody OrderRequest orderRequest) {

        Long userId = SecurityUtils.getCurrentUserId();

        log.info("Creating checkout session for user {} ", userId);

        String sessionId = request.getSession().getId();
        Session stripeSession = orderService.createCheckoutSession(sessionId, userId, orderRequest);

        CheckoutResponse response = CheckoutResponse.builder()
                                                    .checkoutUrl(stripeSession.getUrl())
                                                    .sessionId(stripeSession.getId())
                                                    .orderId(stripeSession.getMetadata().get("order_id"))
                                                    .expiresAt(stripeSession.getExpiresAt())
                                                    .build();

        return ResponseEntity.ok(response);
    }

    /*@PostMapping
    @Operation(summary = "Create order from cart", description = "Create a new order from the current cart contents")
    @ApiResponse(responseCode = "201", description = "Order created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request or empty cart")
    @ApiResponse(responseCode = "409", description = "Tickets no longer available")
    public ResponseEntity<OrderResponse> createOrder(
            HttpServletRequest request,
            @Valid @RequestBody OrderRequest orderRequest) {

        Long userId = SecurityUtils.getCurrentUserId();
        String sessionId = request.getSession().getId();
        Order order = orderService.createOrderFromCart(sessionId, userId, orderRequest);

        OrderResponse response = mapToOrderResponse(order);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);

    }*/

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order by ID", description = "Retrieve a specific order by its ID")
    @ApiResponse(responseCode = "200", description = "Order found")
    @ApiResponse(responseCode = "404", description = "Order not found")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long orderId) {

        OrderResponse response = orderService.getOrderResponseById(orderId);
        return ResponseEntity.ok(response);

    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user orders", description = "Retrieve all orders for a specific user")
    @ApiResponse(responseCode = "200", description = "Orders retrieved successfully")
    public ResponseEntity<Page<OrderResponse>> getUserOrders(
            @PathVariable Long userId,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<OrderResponse> response = orderService.getOrdersByUser(userId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/event/{eventId}")
    @Operation(summary = "Get event orders", description = "Retrieve all orders for a specific event")
    @ApiResponse(responseCode = "200", description = "Orders retrieved successfully")
    public ResponseEntity<Page<OrderResponse>> getEventOrders(
            @PathVariable Long eventId,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<OrderResponse> response = orderService.getOrdersByEvent(eventId, pageable);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel order", description = "Cancel an existing order")
    @ApiResponse(responseCode = "200", description = "Order cancelled successfully")
    @ApiResponse(responseCode = "400", description = "Order cannot be cancelled")
    @ApiResponse(responseCode = "404", description = "Order not found")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable Long orderId,
            @Parameter(description = "Cancellation reason") @RequestParam(required = false) String reason) {

        OrderResponse response = orderService.cancelOrderResponse(orderId, reason);

        return ResponseEntity.ok(response);

    }

}
