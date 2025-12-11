package com.lagab.eventz.app.domain.cart.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.lagab.eventz.app.domain.cart.dto.CartErrorCode;
import com.lagab.eventz.app.domain.cart.dto.CartWarningCode;
import com.lagab.eventz.app.domain.cart.model.Cart;
import com.lagab.eventz.app.domain.cart.model.CartItem;
import com.lagab.eventz.app.domain.cart.model.CartValidationResult;
import com.lagab.eventz.app.domain.event.dto.ticket.TicketTypeDTO;
import com.lagab.eventz.app.domain.event.service.TicketTypeService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartValidationService {

    public static final String TICKET_NAME = "ticketName";
    private final TicketTypeService ticketTypeService;

    public CartValidationResult validateCart(Cart cart) {
        CartValidationResult result = new CartValidationResult(cart);

        // Global cart validations
        validateCartGlobally(cart, result);

        // Individual item validations
        validateCartItems(cart, result);

        return result;
    }

    private void validateCartItems(Cart cart, CartValidationResult result) {
        List<CartItem> itemsToRemove = new ArrayList<>();

        for (CartItem item : cart.getItems()) {
            try {
                TicketTypeDTO ticketType = ticketTypeService.getTicketTypeById(item.getTicketTypeId());

                // Validate ticket availability for sale
                if (!validateTicketOnSale(item, ticketType, result)) {
                    itemsToRemove.add(item);
                    continue;
                }

                // Validate price changes
                validatePriceChanges(item, ticketType, result);

                // Validate stock availability
                if (!validateStockAvailability(item, ticketType, result)) {
                    itemsToRemove.add(item);
                    continue;
                }

                // Validate quantity limits
                validateQuantityLimits(item, ticketType, result);

                // Update ticket information
                updateItemInformation(item, ticketType);

            } catch (EntityNotFoundException e) {
                handleTicketNotFound(item, result);
                itemsToRemove.add(item);
            } catch (Exception e) {
                handleValidationError(item, e, result);
                itemsToRemove.add(item);
            }
        }

        // Remove invalid items
        removeInvalidItems(cart, itemsToRemove, result);
    }

    private boolean validateTicketOnSale(CartItem item, TicketTypeDTO ticketType, CartValidationResult result) {
        if (!ticketType.isOnSale()) {
            result.addError(
                    CartErrorCode.TICKET_NOT_ON_SALE,
                    "Ticket is no longer available for purchase",
                    Map.of(TICKET_NAME, item.getTicketTypeName())
            );
            result.setHasChanges(true);
            return false;
        }
        return true;
    }

    private void validatePriceChanges(CartItem item, TicketTypeDTO ticketType, CartValidationResult result) {
        if (!item.getUnitPrice().equals(ticketType.price())) {
            BigDecimal oldPrice = item.getUnitPrice();
            item.setUnitPrice(ticketType.price());
            item.calculateTotalPrice();
            result.setHasChanges(true);

            if (ticketType.price().compareTo(oldPrice) > 0) {
                result.addWarning(
                        CartWarningCode.PRICE_INCREASED,
                        "Price has increased",
                        Map.of(
                                TICKET_NAME, item.getTicketTypeName(),
                                "oldPrice", oldPrice,
                                "newPrice", ticketType.price()
                        )
                );
            } else {
                result.addWarning(
                        CartWarningCode.PRICE_DECREASED,
                        "Price has decreased",
                        Map.of(
                                TICKET_NAME, item.getTicketTypeName(),
                                "oldPrice", oldPrice,
                                "newPrice", ticketType.price()
                        )
                );
            }
        }
    }

    private boolean validateStockAvailability(CartItem item, TicketTypeDTO ticketType, CartValidationResult result) {
        Integer available = ticketType.remainingTickets();

        if (available != null) {
            if (available == 0) {
                result.addError(
                        CartErrorCode.TICKET_OUT_OF_STOCK,
                        "Ticket is sold out",
                        Map.of(TICKET_NAME, item.getTicketTypeName())
                );
                result.setHasChanges(true);
                return false;
            }

            if (item.getQuantity() > available) {
                int oldQuantity = item.getQuantity();
                item.setQuantity(available);
                item.calculateTotalPrice();
                result.setHasChanges(true);

                result.addWarning(
                        CartWarningCode.QUANTITY_REDUCED_STOCK,
                        "Quantity reduced due to limited stock",
                        Map.of(
                                TICKET_NAME, item.getTicketTypeName(),
                                "oldQuantity", oldQuantity,
                                "newQuantity", available
                        )
                );
            } else if (available <= 5) {
                result.addWarning(
                        CartWarningCode.LOW_STOCK,
                        "Limited stock available",
                        Map.of(
                                TICKET_NAME, item.getTicketTypeName(),
                                "remaining", available
                        )
                );
            }
        }

        return true;
    }

    private void validateQuantityLimits(CartItem item, TicketTypeDTO ticketType, CartValidationResult result) {
        // Minimum quantity validation
        if (item.getQuantity() < ticketType.minQuantity()) {
            result.addError(
                    CartErrorCode.QUANTITY_BELOW_MINIMUM,
                    "Minimum quantity required",
                    Map.of(
                            TICKET_NAME, item.getTicketTypeName(),
                            "minimum", ticketType.minQuantity(),
                            "current", item.getQuantity()
                    )
            );
        }

        // Maximum quantity validation
        if (item.getQuantity() > ticketType.maxQuantity()) {
            int oldQuantity = item.getQuantity();
            item.setQuantity(ticketType.maxQuantity());
            item.calculateTotalPrice();
            result.setHasChanges(true);

            result.addWarning(
                    CartWarningCode.QUANTITY_REDUCED_LIMIT,
                    "Quantity reduced due to order limit",
                    Map.of(
                            TICKET_NAME, item.getTicketTypeName(),
                            "oldQuantity", oldQuantity,
                            "newQuantity", ticketType.maxQuantity()
                    )
            );
        }
    }

    private void updateItemInformation(CartItem item, TicketTypeDTO ticketType) {
        item.setAvailableQuantity(ticketType.remainingTickets());
        item.setMaxQuantityPerOrder(ticketType.maxQuantity());
        item.setMinQuantityPerOrder(ticketType.minQuantity());
    }

    private void handleTicketNotFound(CartItem item, CartValidationResult result) {
        result.addError(
                CartErrorCode.TICKET_NOT_FOUND,
                "Ticket no longer exists",
                Map.of(TICKET_NAME, item.getTicketTypeName())
        );
        result.setHasChanges(true);
    }

    private void handleValidationError(CartItem item, Exception e, CartValidationResult result) {
        log.error("Error validating ticket {}: {}", item.getTicketTypeName(), e.getMessage());
        result.addError(
                CartErrorCode.VALIDATION_ERROR,
                "Error validating ticket",
                Map.of(TICKET_NAME, item.getTicketTypeName())
        );
        result.setHasChanges(true);
    }

    private void removeInvalidItems(Cart cart, List<CartItem> itemsToRemove, CartValidationResult result) {
        for (CartItem itemToRemove : itemsToRemove) {
            cart.removeItem(itemToRemove.getTicketTypeId());
        }

        if (!itemsToRemove.isEmpty()) {
            result.setHasChanges(true);
        }
    }

    private void validateCartGlobally(Cart cart, CartValidationResult result) {
        // Empty cart validation
        if (cart.isEmpty()) {
            result.addError(CartErrorCode.CART_EMPTY, "Your cart is empty");
            return;
        }

        // Mixed events validation
        validateMixedEvents(cart, result);
    }

    private void validateMixedEvents(Cart cart, CartValidationResult result) {
        if (!cart.isEmpty()) {
            Long firstEventId = cart.getItems().getFirst().getEventId();
            boolean mixedEvents = cart.getItems().stream()
                                      .anyMatch(item -> !item.getEventId().equals(firstEventId));

            if (mixedEvents) {
                result.addError(
                        CartErrorCode.MIXED_EVENTS,
                        "Your cart contains tickets from different events"
                );
            }
        }
    }

}
