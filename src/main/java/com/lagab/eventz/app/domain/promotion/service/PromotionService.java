package com.lagab.eventz.app.domain.promotion.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.lagab.eventz.app.common.exception.BusinessException;
import com.lagab.eventz.app.domain.cart.exception.CartException;
import com.lagab.eventz.app.domain.cart.model.Cart;
import com.lagab.eventz.app.domain.cart.model.CartItem;
import com.lagab.eventz.app.domain.event.repository.TicketTypeRepository;
import com.lagab.eventz.app.domain.promotion.model.Discount;
import com.lagab.eventz.app.domain.promotion.model.DiscountCriteria;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private final DiscountService discountService;
    private final TicketTypeRepository ticketTypeRepository;

    /**
     * Apply a promo code to the given cart. If code is null/blank, any existing promo is removed.
     * Throws CartException if the code is invalid or not applicable.
     */
    @Transactional
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

        // Try to resolve a coded Discount from persistence first
        Optional<Discount> opt = discountService.findByCode(normalized);
        if (opt.isPresent()) {
            BigDecimal discountAmount = computeDiscountForCart(opt.get(), cart);
            setPromo(cart, normalized, discountAmount);
            return cart;
        }

        throw new CartException("Invalid promo code");

    }

    /**
     * Recalculate the discount for the cart's existing promo code after cart changes.
     * If no promoCode set, does nothing.
     */
    @Transactional
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

    private BigDecimal computeDiscountForCart(Discount discount, Cart cart) {
        return validateDiscount(discount)
                .map(validDiscount -> calculateDiscountAmount(validDiscount, cart))
                .orElseThrow(() -> new CartException("Invalid discount configuration"));
    }

    private Optional<Discount> validateDiscount(Discount discount) {
        return validateDiscountTiming(discount)
                .flatMap(this::validateDiscountAvailability);
    }

    private Optional<Discount> validateDiscountTiming(Discount discount) {
        LocalDateTime now = LocalDateTime.now();

        return Optional.of(discount)
                       .filter(d -> isDiscountActive(d, now))
                       .or(() -> {
                           throw determineTimingException(discount, now);
                       });
    }

    private boolean isDiscountActive(Discount discount, LocalDateTime now) {
        return Optional.ofNullable(discount.getStartDate())
                       .map(start -> !now.isBefore(start))
                       .orElse(true)
                && Optional.ofNullable(discount.getEndDate())
                           .map(end -> !now.isAfter(end))
                           .orElse(true);
    }

    private CartException determineTimingException(Discount discount, LocalDateTime now) {
        if (discount.getStartDate() != null && now.isBefore(discount.getStartDate())) {
            return new CartException("Promo not yet active");
        }
        if (discount.getEndDate() != null && now.isAfter(discount.getEndDate())) {
            return new CartException("Promo expired");
        }
        return new CartException("Invalid discount timing");
    }

    private Optional<Discount> validateDiscountAvailability(Discount discount) {
        return Optional.of(discount)
                       .filter(this::hasAvailableQuantity)
                       .or(() -> {
                           throw new CartException("Promo usage limit reached");
                       });
    }

    private boolean hasAvailableQuantity(Discount discount) {
        return Optional.ofNullable(discount.getQuantityAvailable())
                       .flatMap(available -> Optional.ofNullable(discount.getQuantitySold())
                                                     .map(sold -> sold < available))
                       .orElse(true);
    }

    private BigDecimal calculateDiscountAmount(Discount discount, Cart cart) {
        BigDecimal eligibleBase = computeEligibleAmount(discount, cart);

        return computeDiscountValue(discount, eligibleBase)
                .filter(amount -> amount.compareTo(BigDecimal.ZERO) > 0)
                .map(computed -> computed.min(eligibleBase))
                .orElseThrow(() -> new CartException("Promo not applicable"));
    }

    private BigDecimal computeEligibleAmount(Discount discount, Cart cart) {
        DiscountCriteria criteria = DiscountCriteria.from(discount);

        BigDecimal filteredAmount = cart.getItems().stream()
                                        .filter(item -> criteria.isEligible(item, ticketTypeRepository))
                                        .map(CartItem::getTotalPrice)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        return Optional.of(filteredAmount)
                       .filter(amount -> amount.compareTo(BigDecimal.ZERO) > 0)
                       .orElseGet(() -> fallbackAmount(criteria, cart));
    }

    private BigDecimal fallbackAmount(DiscountCriteria criteria, Cart cart) {
        return criteria.hasNoRestrictions()
                ? Optional.ofNullable(cart.getSubtotal()).orElse(BigDecimal.ZERO)
                : BigDecimal.ZERO;
    }

    private Optional<BigDecimal> computeDiscountValue(Discount discount, BigDecimal base) {
        return switch (discount.getType()) {
            case PERCENTAGE -> computePercentageDiscount(discount.getPercentOff(), base);
            case FIXED_AMOUNT -> computeFixedDiscount(discount.getAmountOff());
            case null -> Optional.empty();
        };
    }

    private Optional<BigDecimal> computePercentageDiscount(BigDecimal percentOff, BigDecimal base) {
        return Optional.ofNullable(percentOff)
                       .filter(percent -> percent.compareTo(BigDecimal.ZERO) > 0)
                       .map(percent -> base.multiply(percent.movePointLeft(2))
                                           .setScale(2, RoundingMode.HALF_UP));
    }

    private Optional<BigDecimal> computeFixedDiscount(BigDecimal amountOff) {
        return Optional.ofNullable(amountOff)
                       .filter(amount -> amount.compareTo(BigDecimal.ZERO) > 0)
                       .map(amount -> amount.setScale(2, RoundingMode.HALF_UP));
    }
}
