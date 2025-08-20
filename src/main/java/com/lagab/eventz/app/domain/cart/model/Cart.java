package com.lagab.eventz.app.domain.cart.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Cart {
    private String sessionId;
    private Long userId;
    private List<CartItem> items = new ArrayList<>();
    private BigDecimal subtotal = BigDecimal.ZERO;
    private BigDecimal fees = BigDecimal.ZERO;
    private BigDecimal discount = BigDecimal.ZERO;
    private BigDecimal total = BigDecimal.ZERO;
    private String promoCode;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    public void addItem(CartItem item) {
        // Check if cart item already exists
        CartItem existingItem = items.stream()
                                     .filter(i -> i.getTicketTypeId().equals(item.getTicketTypeId()))
                                     .findFirst()
                                     .orElse(null);

        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + item.getQuantity());
            existingItem.calculateTotalPrice();
        } else {
            item.calculateTotalPrice();
            items.add(item);
        }

        calculateTotals();
    }

    public void removeItem(Long ticketTypeId) {
        items.removeIf(item -> item.getTicketTypeId().equals(ticketTypeId));
        calculateTotals();
    }

    public void updateQuantity(Long ticketTypeId, Integer newQuantity) {
        CartItem item = items.stream()
                             .filter(i -> i.getTicketTypeId().equals(ticketTypeId))
                             .findFirst()
                             .orElse(null);

        if (item != null) {
            if (newQuantity <= 0) {
                removeItem(ticketTypeId);
            } else {
                item.setQuantity(newQuantity);
                item.calculateTotalPrice();
                calculateTotals();
            }
        }
    }

    public void calculateTotals() {
        subtotal = items.stream()
                        .map(CartItem::getTotalPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate fees (3% of subtotal + €1 fixed fee)
        fees = subtotal.multiply(BigDecimal.valueOf(0.03))
                       .add(BigDecimal.ONE)
                       .setScale(2, RoundingMode.HALF_UP);

        if (discount == null) {
            discount = BigDecimal.ZERO;
        }
        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            discount = BigDecimal.ZERO;
        }

        BigDecimal gross = subtotal.add(fees);
        if (discount.compareTo(gross) > 0) {
            discount = gross;
        }
        total = gross.subtract(discount).setScale(2, RoundingMode.HALF_UP);
        updatedAt = LocalDateTime.now();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int getTotalItems() {
        return items.stream().mapToInt(CartItem::getQuantity).sum();
    }
}
