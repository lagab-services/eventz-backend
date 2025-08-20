package com.lagab.eventz.app.interfaces.web.cart;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lagab.eventz.app.domain.cart.dto.CartErrorCode;
import com.lagab.eventz.app.domain.cart.dto.CartMessage;
import com.lagab.eventz.app.domain.cart.dto.CartResponse;
import com.lagab.eventz.app.domain.cart.exception.CartException;
import com.lagab.eventz.app.domain.cart.model.Cart;
import com.lagab.eventz.app.domain.cart.model.CartItem;
import com.lagab.eventz.app.domain.cart.model.CartValidationResult;
import com.lagab.eventz.app.domain.cart.service.CartService;
import com.lagab.eventz.app.interfaces.web.cart.mapper.CartMapper;
import com.lagab.eventz.app.util.SecurityUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Shopping cart management")
public class CartController {

    public static final String FIELD = "field";
    public static final String TICKET_TYPE_ID = "ticketTypeId";
    public static final String QUANTITY = "quantity";
    private final CartService cartService;
    private final CartMapper cartMapper;

    @GetMapping
    @Operation(summary = "Get current cart", description = "Retrieve the current shopping cart")
    @ApiResponse(responseCode = "200", description = "Cart retrieved successfully")
    public ResponseEntity<CartResponse> getCart(HttpServletRequest request) {

        String sessionId = getSessionId(request);
        Long userId = SecurityUtils.getCurrentUserIdOrNull();

        Cart cart = cartService.getCart(sessionId, userId);

        return ResponseEntity.ok(cartMapper.toCartResponse(cart));
    }

    @PostMapping("/items")
    @Operation(summary = "Add item to cart", description = "Add a ticket type to the shopping cart")
    @ApiResponse(responseCode = "200", description = "Item added successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request or ticket unavailable")
    public ResponseEntity<CartResponse> addToCart(
            HttpServletRequest request,
            @Parameter(description = "Ticket type ID") @RequestParam Long ticketTypeId,
            @Parameter(description = "Quantity to add") @RequestParam Integer quantity) {

        // Parameter validation
        if (ticketTypeId == null || ticketTypeId <= 0) {
            CartMessage errorMessage = CartMessage.error(
                    CartErrorCode.VALIDATION_ERROR,
                    "Ticket identifier is required",
                    Map.of(FIELD, TICKET_TYPE_ID)
            );
            return ResponseEntity.badRequest().body(cartMapper.createErrorResponse(errorMessage));
        }

        if (quantity == null || quantity <= 0) {
            CartMessage errorMessage = CartMessage.error(
                    CartErrorCode.VALIDATION_ERROR,
                    "Quantity must be greater than 0",
                    Map.of(FIELD, QUANTITY, "value", quantity)
            );
            return ResponseEntity.badRequest().body(cartMapper.createErrorResponse(errorMessage));
        }

        try {
            String sessionId = getSessionId(request);
            Long userId = SecurityUtils.getCurrentUserIdOrNull();
            Cart cart = cartService.addToCart(sessionId, userId, ticketTypeId, quantity);

            return ResponseEntity.ok(mapToCartResponse(cart));
        } catch (CartException e) {
            // Map specific exceptions to appropriate error codes
            CartErrorCode errorCode = mapCartExceptionToErrorCode(e);
            CartMessage errorMessage = CartMessage.error(
                    errorCode,
                    e.getMessage(),
                    Map.of(TICKET_TYPE_ID, ticketTypeId, QUANTITY, quantity)
            );

            CartResponse errorResponse = cartMapper.createErrorResponse(errorMessage);
            return ResponseEntity.badRequest().body(errorResponse);

        }
    }

    @PutMapping("/items/{ticketTypeId}")
    @Operation(summary = "Update cart item", description = "Update the quantity of a specific item in the cart")
    @ApiResponse(responseCode = "200", description = "Item updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid quantity or ticket unavailable")
    public ResponseEntity<CartResponse> updateCartItem(
            HttpServletRequest request,
            @PathVariable Long ticketTypeId,
            @Parameter(description = "New quantity") @RequestParam Integer quantity) {

        // Parameter validation
        if (ticketTypeId == null || ticketTypeId <= 0) {
            CartMessage errorMessage = CartMessage.error(
                    CartErrorCode.VALIDATION_ERROR,
                    "Ticket identifier is required",
                    Map.of(FIELD, TICKET_TYPE_ID)
            );
            return ResponseEntity.badRequest().body(cartMapper.createErrorResponse(errorMessage));
        }

        if (quantity == null || quantity < 0) {
            CartMessage errorMessage = CartMessage.error(
                    CartErrorCode.VALIDATION_ERROR,
                    "Quantity must be greater than or equal to 0",
                    Map.of(FIELD, QUANTITY, "value", quantity)
            );
            return ResponseEntity.badRequest().body(cartMapper.createErrorResponse(errorMessage));
        }

        try {
            String sessionId = getSessionId(request);
            Long userId = SecurityUtils.getCurrentUserIdOrNull();
            Cart cart = cartService.updateCartItem(sessionId, userId, ticketTypeId, quantity);

            return ResponseEntity.ok(mapToCartResponse(cart));
        } catch (CartException e) {
            CartErrorCode errorCode = mapCartExceptionToErrorCode(e);
            CartMessage errorMessage = CartMessage.error(
                    errorCode,
                    e.getMessage(),
                    Map.of(TICKET_TYPE_ID, ticketTypeId, QUANTITY, quantity)
            );

            CartResponse errorResponse = cartMapper.createErrorResponse(errorMessage);
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @DeleteMapping("/items/{ticketTypeId}")
    @Operation(summary = "Remove item from cart", description = "Remove a specific ticket type from the cart")
    @ApiResponse(responseCode = "200", description = "Item removed successfully")
    public ResponseEntity<CartResponse> removeFromCart(
            HttpServletRequest request,
            @PathVariable Long ticketTypeId) {

        String sessionId = getSessionId(request);
        Long userId = SecurityUtils.getCurrentUserIdOrNull();
        Cart cart = cartService.removeFromCart(sessionId, userId, ticketTypeId);

        return ResponseEntity.ok(mapToCartResponse(cart));
    }

    @DeleteMapping
    @Operation(summary = "Clear cart", description = "Remove all items from the cart")
    @ApiResponse(responseCode = "204", description = "Cart cleared successfully")
    public ResponseEntity<Void> clearCart(
            HttpServletRequest request) {

        String sessionId = getSessionId(request);
        Long userId = SecurityUtils.getCurrentUserIdOrNull();
        cartService.clearCart(sessionId, userId);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate and refresh cart", description = "Validate cart contents and update prices/availability")
    @ApiResponse(responseCode = "200", description = "Cart validated successfully")
    public ResponseEntity<CartResponse> validateCart(
            HttpServletRequest request) {

        String sessionId = getSessionId(request);
        Long userId = SecurityUtils.getCurrentUserIdOrNull();
        CartValidationResult validationResult = cartService.validateAndRefreshCart(sessionId, userId);

        return ResponseEntity.ok(cartMapper.toCartResponse(validationResult));
    }

    private String getSessionId(HttpServletRequest request) {
        return request.getSession().getId();
    }

    private CartResponse mapToCartResponse(Cart cart) {
        List<CartResponse.CartItemResponse> items = cart.getItems().stream()
                                                        .map(this::mapToCartItemResponse)
                                                        .toList();

        return new CartResponse(
                items,
                cart.getSubtotal(),
                cart.getFees(),
                cart.getTotal(),
                cart.getTotalItems(),
                cart.getUpdatedAt(),
                true, // isValid - you might want to add validation logic
                new ArrayList<>(), // warnings
                new ArrayList<>()  // errors
        );
    }

    private CartResponse.CartItemResponse mapToCartItemResponse(CartItem item) {
        return new CartResponse.CartItemResponse(
                item.getTicketTypeId(),
                item.getTicketTypeName(),
                item.getEventTitle(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getTotalPrice(),
                item.getAvailableQuantity(),
                item.getAvailableQuantity() != null && item.getAvailableQuantity() > 0
        );
    }

    // Utility method to map exceptions to error codes
    private CartErrorCode mapCartExceptionToErrorCode(CartException e) {
        String message = e.getMessage().toLowerCase();

        if (message.contains("stock") || message.contains("available")) {
            return CartErrorCode.TICKET_OUT_OF_STOCK;
        } else if (message.contains("minimum")) {
            return CartErrorCode.QUANTITY_BELOW_MINIMUM;
        } else if (message.contains("maximum")) {
            return CartErrorCode.QUANTITY_ABOVE_MAXIMUM;
        } else if (message.contains("sale")) {
            return CartErrorCode.TICKET_NOT_ON_SALE;
        } else {
            return CartErrorCode.VALIDATION_ERROR;
        }
    }
}
