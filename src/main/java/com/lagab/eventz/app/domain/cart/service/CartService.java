package com.lagab.eventz.app.domain.cart.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.lagab.eventz.app.domain.cart.exception.CartException;
import com.lagab.eventz.app.domain.cart.model.Cart;
import com.lagab.eventz.app.domain.cart.model.CartItem;
import com.lagab.eventz.app.domain.cart.model.CartValidationResult;
import com.lagab.eventz.app.domain.event.dto.ticket.TicketTypeDTO;
import com.lagab.eventz.app.domain.event.service.TicketTypeService;
import com.lagab.eventz.app.domain.promotion.service.PromotionService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CartService {

    public static final String USER = "user_";
    public static final String SESSION = "session_";

    // In-memory cache for carts (use Redis in production)
    private final Map<String, Cart> cartCache = new ConcurrentHashMap<>();

    private final TicketTypeService ticketTypeService;
    private final CartValidationService cartValidationService;
    private final PromotionService promotionService;

    public Cart getOrCreateCart(String sessionId, Long userId) {
        String cartKey = userId != null ? USER + userId : SESSION + sessionId;
        return cartCache.computeIfAbsent(cartKey, k -> {
            Cart cart = new Cart();
            cart.setSessionId(sessionId);
            cart.setUserId(userId);
            return cart;
        });
    }

    public Cart addToCart(String sessionId, Long userId, Long ticketTypeId, Integer quantity) {
        TicketTypeDTO ticketType = ticketTypeService.getTicketTypeById(ticketTypeId);
        validateTicketAvailability(ticketType, quantity);

        Cart cart = getOrCreateCart(sessionId, userId);

        CartItem cartItem = new CartItem();
        cartItem.setTicketTypeId(ticketTypeId);
        cartItem.setTicketTypeName(ticketType.name());
        cartItem.setUnitPrice(ticketType.price());
        cartItem.setQuantity(quantity);
        cartItem.setEventId(ticketType.eventId());
        cartItem.setEventTitle(ticketType.eventName());
        cartItem.setAvailableQuantity(ticketType.remainingTickets());
        cartItem.setMaxQuantityPerOrder(ticketType.maxQuantity());
        cartItem.setMinQuantityPerOrder(ticketType.minQuantity());

        cart.addItem(cartItem);
        saveCart(cart);

        return cart;
    }

    public Cart removeFromCart(String sessionId, Long userId, Long ticketTypeId) {
        Cart cart = getOrCreateCart(sessionId, userId);
        cart.removeItem(ticketTypeId);
        saveCart(cart);
        return cart;
    }

    public Cart updateCartItem(String sessionId, Long userId, Long ticketTypeId, Integer quantity) {
        TicketTypeDTO ticketType = ticketTypeService.getTicketTypeById(ticketTypeId);

        if (quantity > 0) {
            validateTicketAvailability(ticketType, quantity);
        }

        Cart cart = getOrCreateCart(sessionId, userId);
        cart.updateQuantity(ticketTypeId, quantity);
        saveCart(cart);

        return cart;
    }

    public Cart getCart(String sessionId, Long userId) {
        return getOrCreateCart(sessionId, userId);
    }

    public void clearCart(String sessionId, Long userId) {
        String cartKey = userId != null ? USER + userId : SESSION + sessionId;
        cartCache.remove(cartKey);
    }

    public CartValidationResult validateAndRefreshCart(String sessionId, Long userId) {
        Cart cart = getOrCreateCart(sessionId, userId);
        CartValidationResult result = cartValidationService.validateCart(cart);

        // Recalculate promo if anything changed or as a safety measure
        promotionService.recalculate(cart);

        if (result.isHasChanges()) {
            cart.calculateTotals();
            saveCart(cart);
        } else {
            // ensure cart is persisted if promo recalculated changed totals
            saveCart(cart);
        }
        return result;
    }

    private void validateTicketAvailability(TicketTypeDTO ticketType, Integer requestedQuantity) {
        if (!ticketType.isOnSale()) {
            throw new CartException("This ticket is no longer available for purchase");
        }

        if (requestedQuantity < ticketType.minQuantity()) {
            throw new CartException("Minimum quantity required: " + ticketType.minQuantity());
        }

        if (requestedQuantity > ticketType.maxQuantity()) {
            throw new CartException("Maximum quantity allowed: " + ticketType.maxQuantity());
        }

        Integer available = ticketType.remainingTickets();
        if (available != null && requestedQuantity > available) {
            throw new CartException("Only " + available + " tickets available");
        }
    }

    private void saveCart(Cart cart) {
        String cartKey = cart.getUserId() != null ? USER + cart.getUserId() : SESSION + cart.getSessionId();
        cartCache.put(cartKey, cart);
    }

    public Cart applyPromoCode(String sessionId, Long userId, String promoCode) {
        Cart cart = getOrCreateCart(sessionId, userId);
        promotionService.applyPromoCode(cart, promoCode);
        saveCart(cart);
        return cart;
    }
}
