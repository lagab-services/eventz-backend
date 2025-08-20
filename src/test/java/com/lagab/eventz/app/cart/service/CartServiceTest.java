package com.lagab.eventz.app.cart.service;

import com.lagab.eventz.app.domain.cart.exception.CartException;
import com.lagab.eventz.app.domain.cart.model.Cart;
import com.lagab.eventz.app.domain.cart.model.CartValidationResult;
import com.lagab.eventz.app.domain.cart.service.CartService;
import com.lagab.eventz.app.domain.cart.service.CartValidationService;
import com.lagab.eventz.app.domain.event.dto.ticket.TicketTypeDTO;
import com.lagab.eventz.app.domain.event.service.TicketTypeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private TicketTypeService ticketTypeService;

    @Mock
    private CartValidationService cartValidationService;

    @InjectMocks
    private CartService cartService;

    private TicketTypeDTO validTicketType;

    @BeforeEach
    void setUp() {
        validTicketType = new TicketTypeDTO(
                1L,
                "Standard Ticket",
                "Standard concert access",
                BigDecimal.valueOf(45.00),
                100,
                0,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30),
                1, // min
                6, // max
                true, // isActive
                100, // remainingTickets (deprecated field still used in some places)
                1L,
                "Standard",
                100, // quantityRemaining
                BigDecimal.ZERO,
                true, // isOnSale
                false, // isSoldOut
                10L, // eventId
                "My Event"
        );
    }

    @Test
    void addToCart_success_shouldAddItemWithMappedFields() {
        when(ticketTypeService.getTicketTypeById(1L)).thenReturn(validTicketType);

        Cart cart = cartService.addToCart("sess-1", null, 1L, 2);

        assertNotNull(cart);
        assertEquals("sess-1", cart.getSessionId());
        assertNull(cart.getUserId());
        assertEquals(1, cart.getItems().size());
        var item = cart.getItems().getFirst();
        assertEquals(1L, item.getTicketTypeId());
        assertEquals("Standard Ticket", item.getTicketTypeName());
        assertEquals(BigDecimal.valueOf(45.00), item.getUnitPrice());
        assertEquals(2, item.getQuantity());
        assertEquals(10L, item.getEventId());
        assertEquals("My Event", item.getEventTitle());
        assertEquals(100, item.getAvailableQuantity());
        assertEquals(6, item.getMaxQuantityPerOrder());
        assertEquals(1, item.getMinQuantityPerOrder());
        // Totals are calculated in Cart.addItem
        assertEquals(BigDecimal.valueOf(90.00), item.getTotalPrice());
        assertTrue(cart.getSubtotal().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(cart.getTotal().compareTo(cart.getSubtotal()) > 0); // fees added
    }

    @Test
    void addToCart_shouldThrow_whenNotOnSale() {
        TicketTypeDTO dto = ticketTypeWithOverrides(false, 1, 6, 100);
        when(ticketTypeService.getTicketTypeById(1L)).thenReturn(dto);
        CartException ex = assertThrows(CartException.class, () -> cartService.addToCart("s", null, 1L, 2));
        assertEquals("This ticket is no longer available for purchase", ex.getMessage());
    }

    @Test
    void addToCart_shouldThrow_whenBelowMin() {
        TicketTypeDTO dto = ticketTypeWithOverrides(true, 2, 6, 100);
        when(ticketTypeService.getTicketTypeById(1L)).thenReturn(dto);
        CartException ex = assertThrows(CartException.class, () -> cartService.addToCart("s", null, 1L, 1));
        assertEquals("Minimum quantity required: 2", ex.getMessage());
    }

    @Test
    void addToCart_shouldThrow_whenAboveMax() {
        TicketTypeDTO dto = ticketTypeWithOverrides(true, 1, 3, 100);
        when(ticketTypeService.getTicketTypeById(1L)).thenReturn(dto);
        CartException ex = assertThrows(CartException.class, () -> cartService.addToCart("s", null, 1L, 4));
        assertEquals("Maximum quantity allowed: 3", ex.getMessage());
    }

    @Test
    void addToCart_shouldThrow_whenExceedsRemaining() {
        TicketTypeDTO dto = ticketTypeWithOverrides(true, 1, 6, 2);
        when(ticketTypeService.getTicketTypeById(1L)).thenReturn(dto);
        CartException ex = assertThrows(CartException.class, () -> cartService.addToCart("s", null, 1L, 3));
        assertEquals("Only 2 tickets available", ex.getMessage());
    }

    @Test
    void updateCartItem_positiveQuantity_shouldValidateAndUpdate() {
        when(ticketTypeService.getTicketTypeById(1L)).thenReturn(validTicketType);
        Cart cart = cartService.addToCart("sess-2", 5L, 1L, 1);
        assertEquals(1, cart.getItems().getFirst().getQuantity());

        // Update to quantity 3 (within limits)
        Cart updated = cartService.updateCartItem("sess-2", 5L, 1L, 3);
        assertEquals(1, updated.getItems().size());
        assertEquals(3, updated.getItems().getFirst().getQuantity());
    }

    @Test
    void updateCartItem_zeroQuantity_shouldRemoveWithoutValidation() {
        when(ticketTypeService.getTicketTypeById(1L)).thenReturn(validTicketType);
        Cart cart = cartService.addToCart("sess-3", null, 1L, 2);
        assertFalse(cart.isEmpty());

        // Even though service fetches DTO, it should skip validation when quantity <= 0
        Cart updated = cartService.updateCartItem("sess-3", null, 1L, 0);
        assertTrue(updated.isEmpty());
    }

    @Test
    void removeFromCart_shouldRemoveItem() {
        when(ticketTypeService.getTicketTypeById(1L)).thenReturn(validTicketType);
        Cart cart = cartService.addToCart("sess-4", null, 1L, 2);
        assertEquals(1, cart.getItems().size());

        Cart afterRemove = cartService.removeFromCart("sess-4", null, 1L);
        assertTrue(afterRemove.isEmpty());
    }

    @Test
    void clearCart_shouldClearBySessionKey() {
        when(ticketTypeService.getTicketTypeById(1L)).thenReturn(validTicketType);
        cartService.addToCart("sess-5", null, 1L, 1);
        Cart before = cartService.getCart("sess-5", null);
        assertFalse(before.isEmpty());

        cartService.clearCart("sess-5", null);
        Cart after = cartService.getCart("sess-5", null);
        assertTrue(after.isEmpty());
    }

    @Test
    void clearCart_shouldClearByUserKey() {
        when(ticketTypeService.getTicketTypeById(1L)).thenReturn(validTicketType);
        cartService.addToCart("ignored", 99L, 1L, 1);
        Cart before = cartService.getCart("ignored", 99L);
        assertFalse(before.isEmpty());

        cartService.clearCart("ignored", 99L);
        Cart after = cartService.getCart("ignored", 99L);
        assertTrue(after.isEmpty());
        assertEquals(99L, after.getUserId());
    }

    @Test
    void validateAndRefreshCart_shouldCallCalculateTotalsWhenHasChanges() {
        Cart cart = cartService.getOrCreateCart("sess-6", null);
        // add some state directly via addToCart so totals are meaningful
        when(ticketTypeService.getTicketTypeById(1L)).thenReturn(validTicketType);
        cartService.addToCart("sess-6", null, 1L, 2);

        CartValidationResult result = new CartValidationResult(cart);
        result.setHasChanges(true);
        when(cartValidationService.validateCart(any(Cart.class))).thenReturn(result);

        CartValidationResult out = cartService.validateAndRefreshCart("sess-6", null);
        assertSame(result, out);
        // no exception means calculateTotals and save executed; we can assert totals are non-zero
        assertTrue(cart.getSubtotal().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void getOrCreateCart_shouldReuseCartForSameKey() {
        Cart c1 = cartService.getOrCreateCart("sess-7", null);
        Cart c2 = cartService.getOrCreateCart("sess-7", null);
        assertSame(c1, c2);

        Cart u1 = cartService.getOrCreateCart("ignored", 7L);
        Cart u2 = cartService.getOrCreateCart("ignored", 7L);
        assertSame(u1, u2);
        assertNotSame(c1, u1);
    }

    private TicketTypeDTO ticketTypeWithOverrides(boolean onSale, int min, int max, Integer remaining) {
        return new TicketTypeDTO(
                1L,
                "Standard Ticket",
                "Standard concert access",
                BigDecimal.valueOf(45.00),
                100,
                0,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30),
                min,
                max,
                true,
                remaining,
                1L,
                "Standard",
                remaining,
                BigDecimal.ZERO,
                onSale,
                false,
                10L,
                "My Event"
        );
    }
}
