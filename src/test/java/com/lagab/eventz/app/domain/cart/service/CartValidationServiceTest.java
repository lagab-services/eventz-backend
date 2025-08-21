package com.lagab.eventz.app.domain.cart.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lagab.eventz.app.domain.cart.model.Cart;
import com.lagab.eventz.app.domain.cart.model.CartItem;
import com.lagab.eventz.app.domain.cart.model.CartValidationResult;
import com.lagab.eventz.app.domain.event.dto.ticket.TicketTypeDTO;
import com.lagab.eventz.app.domain.event.service.TicketTypeService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartValidationServiceTest {

    @Mock
    private TicketTypeService ticketTypeService;

    @InjectMocks
    private CartValidationService cartValidationService;

    private Cart cart;
    private CartItem cartItem;
    private TicketTypeDTO ticketTypeDTO;

    @BeforeEach
    void setUp() {
        cart = new Cart();
        cart.setItems(new ArrayList<>());

        cartItem = new CartItem();
        cartItem.setTicketTypeId(1L);
        cartItem.setTicketTypeName("Standard Ticket");
        cartItem.setUnitPrice(BigDecimal.valueOf(45.00));
        cartItem.setQuantity(2);
        cartItem.setEventId(1L);
        cartItem.setEventTitle("Test Concert");

        // Create TicketTypeDTO with all required fields
        ticketTypeDTO = new TicketTypeDTO(
                1L, // id
                "Standard Ticket", // name
                "Standard concert access", // description
                BigDecimal.valueOf(45.00), // price
                100, // quantityAvailable
                0, // quantitySold
                LocalDateTime.now().minusDays(1), // saleStart (started yesterday)
                LocalDateTime.now().plusDays(30), // saleEnd (ends in 30 days)
                1, // minQuantity
                6, // maxQuantity
                true, // isActive
                100, // remainingTickets (deprecated but still used)
                1L, // categoryId
                "Standard", // categoryName
                100, // quantityRemaining
                BigDecimal.ZERO, // totalPrice
                true, // isOnSale
                false, // isSoldOut
                1L, // eventId
                "Test Concert" // eventName
        );
    }

    @Test
    void validateCart_WithValidCart_ShouldReturnValidResult() {
        // Given
        cart.addItem(cartItem);
        when(ticketTypeService.getTicketTypeById(1L)).thenReturn(ticketTypeDTO);

        // When
        CartValidationResult result = cartValidationService.validateCart(cart);

        // Then
        assertTrue(result.isValid());
        assertFalse(result.hasErrors());
        assertFalse(result.hasWarnings());
        assertFalse(result.isHasChanges());
    }

    @Test
    void validateCart_WithPriceIncrease_ShouldAddWarning() {
        // Given
        cart.addItem(cartItem);
        TicketTypeDTO updatedTicketType = new TicketTypeDTO(
                1L, // id
                "Standard Ticket", // name
                "Standard concert access", // description
                BigDecimal.valueOf(50.00), // increased price
                100, // quantityAvailable
                0, // quantitySold
                LocalDateTime.now().minusDays(1), // saleStart
                LocalDateTime.now().plusDays(30), // saleEnd
                1, // minQuantity
                6, // maxQuantity
                true, // isActive
                100, // remainingTickets
                1L, // categoryId
                "Standard", // categoryName
                100, // quantityRemaining
                BigDecimal.ZERO, // totalPrice
                true, // isOnSale
                false, // isSoldOut
                1L, // eventId
                "Test Concert" // eventName
        );
        when(ticketTypeService.getTicketTypeById(1L)).thenReturn(updatedTicketType);

        // When
        CartValidationResult result = cartValidationService.validateCart(cart);

        // Then
        assertTrue(result.isValid());
        assertFalse(result.hasErrors());
        assertTrue(result.hasWarnings());
        assertTrue(result.isHasChanges());
        assertEquals(1, result.getWarnings().size());
        assertEquals("price.increased", result.getWarnings().getFirst().getCode());

        // Verify price was updated in cart
        assertEquals(BigDecimal.valueOf(50.00), cart.getItems().getFirst().getUnitPrice());
    }

    @Test
    void validateCart_WithOutOfStock_ShouldAddError() {
        // Given
        cart.addItem(cartItem);
        TicketTypeDTO outOfStockTicket = new TicketTypeDTO(
                1L, // id
                "Standard Ticket", // name
                "Standard concert access", // description
                BigDecimal.valueOf(45.00), // price
                100, // quantityAvailable
                100, // quantitySold (all sold)
                LocalDateTime.now().minusDays(1), // saleStart
                LocalDateTime.now().plusDays(30), // saleEnd
                1, // minQuantity
                6, // maxQuantity
                true, // isActive
                0, // remainingTickets (stock at 0)
                1L, // categoryId
                "Standard", // categoryName
                0, // quantityRemaining (stock at 0)
                BigDecimal.valueOf(4500.00), // totalPrice
                true, // isOnSale
                true, // isSoldOut
                1L, // eventId
                "Test Concert" // eventName
        );
        when(ticketTypeService.getTicketTypeById(1L)).thenReturn(outOfStockTicket);

        // When
        CartValidationResult result = cartValidationService.validateCart(cart);

        // Then
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
        assertTrue(result.isHasChanges());
        assertEquals(1, result.getErrors().size());
        assertEquals("ticket.out_of_stock", result.getErrors().getFirst().getCode());
        assertTrue(cart.isEmpty()); // Item should be removed
    }

    @Test
    void validateCart_WithTicketNotOnSale_ShouldAddError() {
        // Given
        cart.addItem(cartItem);
        TicketTypeDTO notOnSaleTicket = new TicketTypeDTO(
                1L, // id
                "Standard Ticket", // name
                "Standard concert access", // description
                BigDecimal.valueOf(45.00), // price
                100, // quantityAvailable
                0, // quantitySold
                LocalDateTime.now().plusDays(1), // saleStart (starts tomorrow)
                LocalDateTime.now().plusDays(30), // saleEnd
                1, // minQuantity
                6, // maxQuantity
                true, // isActive
                100, // remainingTickets
                1L, // categoryId
                "Standard", // categoryName
                100, // quantityRemaining
                BigDecimal.ZERO, // totalPrice
                false, // isOnSale (not on sale)
                false, // isSoldOut
                1L, // eventId
                "Test Concert" // eventName
        );
        when(ticketTypeService.getTicketTypeById(1L)).thenReturn(notOnSaleTicket);

        // When
        CartValidationResult result = cartValidationService.validateCart(cart);

        // Then
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
        assertTrue(result.isHasChanges());
        assertEquals(1, result.getErrors().size());
        assertEquals("ticket.not_on_sale", result.getErrors().getFirst().getCode());
        assertTrue(cart.isEmpty()); // Item should be removed
    }

    @Test
    void validateCart_WithQuantityAboveMaximum_ShouldReduceQuantityAndAddWarning() {
        // Given
        cartItem.setQuantity(10); // Above maximum of 6
        cart.addItem(cartItem);
        when(ticketTypeService.getTicketTypeById(1L)).thenReturn(ticketTypeDTO);

        // When
        CartValidationResult result = cartValidationService.validateCart(cart);

        // Then
        assertTrue(result.isValid());
        assertFalse(result.hasErrors());
        assertTrue(result.hasWarnings());
        assertTrue(result.isHasChanges());
        assertEquals(1, result.getWarnings().size());
        assertEquals("quantity.reduced.limit", result.getWarnings().getFirst().getCode());

        // Verify quantity was reduced to maximum allowed
        assertEquals(6, cart.getItems().getFirst().getQuantity());
    }

    @Test
    void validateCart_WithQuantityBelowMinimum_ShouldAddError() {
        // Given
        cartItem.setQuantity(0); // Below minimum of 1
        cart.addItem(cartItem);
        when(ticketTypeService.getTicketTypeById(1L)).thenReturn(ticketTypeDTO);

        // When
        CartValidationResult result = cartValidationService.validateCart(cart);

        // Then
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
        assertEquals(1, result.getErrors().size());
        assertEquals("quantity.below_minimum", result.getErrors().getFirst().getCode());
    }

    @Test
    void validateCart_WithLowStock_ShouldAddWarning() {
        // Given
        cart.addItem(cartItem);
        TicketTypeDTO lowStockTicket = new TicketTypeDTO(
                1L, // id
                "Standard Ticket", // name
                "Standard concert access", // description
                BigDecimal.valueOf(45.00), // price
                100, // quantityAvailable
                97, // quantitySold (only 3 left)
                LocalDateTime.now().minusDays(1), // saleStart
                LocalDateTime.now().plusDays(30), // saleEnd
                1, // minQuantity
                6, // maxQuantity
                true, // isActive
                3, // remainingTickets (low stock)
                1L, // categoryId
                "Standard", // categoryName
                3, // quantityRemaining (low stock)
                BigDecimal.valueOf(4365.00), // totalPrice
                true, // isOnSale
                false, // isSoldOut
                1L, // eventId
                "Test Concert" // eventName
        );
        when(ticketTypeService.getTicketTypeById(1L)).thenReturn(lowStockTicket);

        // When
        CartValidationResult result = cartValidationService.validateCart(cart);

        // Then
        assertTrue(result.isValid());
        assertFalse(result.hasErrors());
        assertTrue(result.hasWarnings());
        assertEquals(1, result.getWarnings().size());
        assertEquals("stock.low", result.getWarnings().getFirst().getCode());
    }

    @Test
    void validateCart_WithEmptyCart_ShouldAddError() {
        // Given - empty cart

        // When
        CartValidationResult result = cartValidationService.validateCart(cart);

        // Then
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
        assertEquals(1, result.getErrors().size());
        assertEquals("cart.empty", result.getErrors().getFirst().getCode());
    }

    @Test
    void validateCart_WithMixedEvents_ShouldAddError() {
        // Given
        CartItem item1 = new CartItem();
        item1.setTicketTypeId(1L);
        item1.setTicketTypeName("Concert Ticket");
        item1.setEventId(1L);
        item1.setUnitPrice(BigDecimal.valueOf(45.00));
        item1.setQuantity(1);

        CartItem item2 = new CartItem();
        item2.setTicketTypeId(2L);
        item2.setTicketTypeName("Theater Ticket");
        item2.setEventId(2L); // Different event
        item2.setUnitPrice(BigDecimal.valueOf(45.00));
        item2.setQuantity(1);

        cart.addItem(item1);
        cart.addItem(item2);

        TicketTypeDTO ticketType1 = createValidTicketTypeDTO(1L, "Concert", 1L);
        TicketTypeDTO ticketType2 = createValidTicketTypeDTO(2L, "Theater", 2L);

        when(ticketTypeService.getTicketTypeById(1L)).thenReturn(ticketType1);
        when(ticketTypeService.getTicketTypeById(2L)).thenReturn(ticketType2);

        // When
        CartValidationResult result = cartValidationService.validateCart(cart);

        // Then
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
        assertTrue(result.getErrors().stream()
                         .anyMatch(error -> "cart.mixed_events".equals(error.getCode())));
    }

    // Helper method to create valid TicketTypeDTO
    private TicketTypeDTO createValidTicketTypeDTO(Long id, String eventName, Long eventId) {
        return new TicketTypeDTO(
                id, // id
                eventName + " Ticket", // name
                "Access to " + eventName, // description
                BigDecimal.valueOf(45.00), // price
                100, // quantityAvailable
                0, // quantitySold
                LocalDateTime.now().minusDays(1), // saleStart
                LocalDateTime.now().plusDays(30), // saleEnd
                1, // minQuantity
                6, // maxQuantity
                true, // isActive
                100, // remainingTickets
                1L, // categoryId
                "Standard", // categoryName
                100, // quantityRemaining
                BigDecimal.ZERO, // totalPrice
                true, // isOnSale
                false, // isSoldOut
                eventId, // eventId
                eventName // eventName
        );
    }

    @Test
    void validateCart_WithQuantityExceedingStock_ShouldReduceQuantityAndAddWarning() {
        // Given
        cartItem.setQuantity(5); // Requesting 5 tickets
        cart.addItem(cartItem);

        TicketTypeDTO limitedStockTicket = new TicketTypeDTO(
                1L, // id
                "Standard Ticket", // name
                "Standard concert access", // description
                BigDecimal.valueOf(45.00), // price
                100, // quantityAvailable
                97, // quantitySold
                LocalDateTime.now().minusDays(1), // saleStart
                LocalDateTime.now().plusDays(30), // saleEnd
                1, // minQuantity
                6, // maxQuantity
                true, // isActive
                3, // remainingTickets (only 3 available)
                1L, // categoryId
                "Standard", // categoryName
                3, // quantityRemaining (only 3 available)
                BigDecimal.valueOf(4365.00), // totalPrice
                true, // isOnSale
                false, // isSoldOut
                1L, // eventId
                "Test Concert" // eventName
        );
        when(ticketTypeService.getTicketTypeById(1L)).thenReturn(limitedStockTicket);

        // When
        CartValidationResult result = cartValidationService.validateCart(cart);

        // Then
        assertTrue(result.isValid());
        assertFalse(result.hasErrors());
        assertTrue(result.hasWarnings());
        assertTrue(result.isHasChanges());

        // Verify quantity was reduced to available stock
        assertEquals(3, cart.getItems().getFirst().getQuantity());

        // Verify warning message
        assertEquals(1, result.getWarnings().size());
        assertEquals("quantity.reduced.stock", result.getWarnings().getFirst().getCode());

        // Verify message parameters
        Map<String, Object> parameters = result.getWarnings().getFirst().getParameters();
        assertEquals("Standard Ticket", parameters.get("ticketName"));
        assertEquals(5, parameters.get("oldQuantity"));
        assertEquals(3, parameters.get("newQuantity"));
    }

    @Test
    void validateCart_WithInactiveTicket_ShouldAddError() {
        // Given
        cart.addItem(cartItem);
        TicketTypeDTO inactiveTicket = new TicketTypeDTO(
                1L, // id
                "Standard Ticket", // name
                "Standard concert access", // description
                BigDecimal.valueOf(45.00), // price
                100, // quantityAvailable
                0, // quantitySold
                LocalDateTime.now().minusDays(1), // saleStart
                LocalDateTime.now().plusDays(30), // saleEnd
                1, // minQuantity
                6, // maxQuantity
                false, // isActive (inactive)
                100, // remainingTickets
                1L, // categoryId
                "Standard", // categoryName
                100, // quantityRemaining
                BigDecimal.ZERO, // totalPrice
                false, // isOnSale (not on sale because inactive)
                false, // isSoldOut
                1L, // eventId
                "Test Concert" // eventName
        );
        when(ticketTypeService.getTicketTypeById(1L)).thenReturn(inactiveTicket);

        // When
        CartValidationResult result = cartValidationService.validateCart(cart);

        // Then
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
        assertTrue(result.isHasChanges());
        assertEquals(1, result.getErrors().size());
        assertEquals("ticket.not_on_sale", result.getErrors().getFirst().getCode());
        assertTrue(cart.isEmpty()); // Item should be removed
    }

    @Test
    void validateCart_WithMultipleIssues_ShouldHandleAllCorrectly() {
        // Given - Create cart with multiple issues
        CartItem item1 = new CartItem();
        item1.setTicketTypeId(1L);
        item1.setTicketTypeName("Standard Ticket");
        item1.setUnitPrice(BigDecimal.valueOf(45.00));
        item1.setQuantity(2);
        item1.setEventId(1L);

        CartItem item2 = new CartItem();
        item2.setTicketTypeId(2L);
        item2.setTicketTypeName("VIP Ticket");
        item2.setUnitPrice(BigDecimal.valueOf(100.00));
        item2.setQuantity(8); // Above maximum
        item2.setEventId(1L);

        cart.addItem(item1);
        cart.addItem(item2);

        // Ticket 1: price change
        TicketTypeDTO ticket1 = new TicketTypeDTO(
                1L, "Standard Ticket", "Standard", BigDecimal.valueOf(50.00), // increased price
                100, 0, LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(30),
                1, 6, true, 100, 1L, "Standard", 100, BigDecimal.ZERO, true, false, 1L, "Test Concert"
        );

        // Ticket 2: quantity above maximum
        TicketTypeDTO ticket2 = new TicketTypeDTO(
                2L, "VIP Ticket", "VIP", BigDecimal.valueOf(100.00),
                50, 0, LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(30),
                1, 4, true, 50, 1L, "VIP", 50, BigDecimal.ZERO, true, false, 1L, "Test Concert"
        );

        when(ticketTypeService.getTicketTypeById(1L)).thenReturn(ticket1);
        when(ticketTypeService.getTicketTypeById(2L)).thenReturn(ticket2);

        // When
        CartValidationResult result = cartValidationService.validateCart(cart);

        // Then
        assertTrue(result.isValid()); // Still valid because no blocking errors
        assertFalse(result.hasErrors());
        assertTrue(result.hasWarnings());
        assertTrue(result.isHasChanges());

        // Verify there are 2 warnings
        assertEquals(2, result.getWarnings().size());

        // Verify warning codes
        assertTrue(result.getWarnings().stream()
                         .anyMatch(w -> "price.increased".equals(w.getCode())));
        assertTrue(result.getWarnings().stream()
                         .anyMatch(w -> "quantity.reduced.limit".equals(w.getCode())));

        // Verify corrections were applied
        assertEquals(BigDecimal.valueOf(50.00), cart.getItems().getFirst().getUnitPrice()); // Price updated
        assertEquals(4, cart.getItems().get(1).getQuantity()); // Quantity reduced to maximum
    }
}
