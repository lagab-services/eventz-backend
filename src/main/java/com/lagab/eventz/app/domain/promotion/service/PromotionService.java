package com.lagab.eventz.app.domain.promotion.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.lagab.eventz.app.common.exception.BusinessException;
import com.lagab.eventz.app.domain.cart.exception.CartException;
import com.lagab.eventz.app.domain.cart.model.Cart;

import lombok.RequiredArgsConstructor;

/**
 * Simple promotion service to validate and apply promo codes to the cart.
 * In a real-world scenario, codes would be persisted with rules/validity windows, usage limits, etc.
 */
@Service
@RequiredArgsConstructor
public class PromotionService {

    /**
     * Apply a promo code to the given cart. If code is null/blank, any existing promo is removed.
     * Throws CartException if the code is invalid or not applicable.
     */
    public Cart applyPromoCode(Cart cart, String code) {
        if (cart == null) {
            throw new BusinessException("cart must not be null");
        }

        if (code == null || code.isBlank()) {
            // Remove promo
            cart.setPromoCode(null);
            cart.setDiscount(BigDecimal.ZERO);
            cart.calculateTotals();
            return cart;
        }

        String normalized = code.trim().toUpperCase(Locale.ROOT);
        switch (normalized) {
        case "PROMO10":
            // 10% off subtotal
            BigDecimal tenPercent = cart.getSubtotal().multiply(BigDecimal.valueOf(0.10))
                                        .setScale(2, RoundingMode.HALF_UP);
            setPromo(cart, normalized, tenPercent);
            return cart;
        case "FREEFEE":
            // Waive fees fully
            setPromo(cart, normalized, cart.getFees());
            return cart;
        case "FIX5":
            // Fixed 5 EUR off total
            setPromo(cart, normalized, BigDecimal.valueOf(5).setScale(2, RoundingMode.HALF_UP));
            return cart;
        default:
            throw new CartException("Invalid promo code");
        }
    }

    /**
     * Recalculate the discount for the cart's existing promo code after cart changes.
     * If no promoCode set, does nothing.
     */
    public void recalculate(Cart cart) {
        if (cart.getPromoCode() == null || cart.getPromoCode().isBlank()) {
            return;
        }
        // Re-apply the same code with new amounts. If it becomes invalid, clear it.
        try {
            applyPromoCode(cart, cart.getPromoCode());
        } catch (CartException e) {
            cart.setPromoCode(null);
            cart.setDiscount(BigDecimal.ZERO);
            cart.calculateTotals();
        }
    }

    private void setPromo(Cart cart, String code, BigDecimal discount) {
        if (discount == null || discount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CartException("Promo not applicable");
        }
        cart.setPromoCode(code);
        cart.setDiscount(discount);
        cart.calculateTotals();
    }
}
