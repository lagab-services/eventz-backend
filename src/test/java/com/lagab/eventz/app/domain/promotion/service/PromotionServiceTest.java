package com.lagab.eventz.app.domain.promotion.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lagab.eventz.app.common.exception.BusinessException;
import com.lagab.eventz.app.domain.cart.exception.CartException;
import com.lagab.eventz.app.domain.cart.model.Cart;
import com.lagab.eventz.app.domain.cart.model.CartItem;
import com.lagab.eventz.app.domain.event.model.Event;
import com.lagab.eventz.app.domain.event.repository.TicketTypeRepository;
import com.lagab.eventz.app.domain.promotion.model.Discount;
import com.lagab.eventz.app.domain.promotion.model.DiscountType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;
import static org.mockito.BDDMockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("PromotionService Tests")
class PromotionServiceTest {

    @Mock
    private DiscountService discountService;
    @Mock
    private TicketTypeRepository ticketTypeRepository;

    @InjectMocks
    private PromotionService promotionService;

    private Cart cart;

    @BeforeEach
    void setUp() {
        cart = createCart();
    }

    @Nested
    @DisplayName("applyPromoCode Tests")
    class ApplyPromoCodeTests {

        @Test
        @DisplayName("Should throw BusinessException when cart is null")
        void shouldThrowBusinessExceptionWhenCartIsNull() {
            // When & Then
            assertThatThrownBy(() -> promotionService.applyPromoCode(null, "PROMO10"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("cart must not be null");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { " ", "\t", "\n" })
        @DisplayName("Should remove promo when code is null, empty or blank")
        void shouldRemovePromoWhenCodeIsNullEmptyOrBlank(String code) {
            // Given
            cart.setPromoCode("EXISTING");
            cart.setDiscount(BigDecimal.valueOf(10));

            // When
            Cart result = promotionService.applyPromoCode(cart, code);

            // Then
            assertThat(result.getPromoCode()).isNull();
            assertThat(result.getDiscount()).isEqualByComparingTo(BigDecimal.ZERO);
            verifyNoInteractions(discountService);
        }

        @Test
        @DisplayName("Should apply database discount when code exists")
        void shouldApplyDatabaseDiscountWhenCodeExists() {
            // Given
            String code = "DATABASE";
            Discount databaseDiscount = createFixedAmountDiscount("1", code, BigDecimal.valueOf(20));

            // Add items consistent with subtotal
            CartItem item1 = createCartItem(1L, 1L, BigDecimal.valueOf(70));
            CartItem item2 = createCartItem(2L, 2L, BigDecimal.valueOf(30));
            cart.setItems(List.of(item1, item2));
            cart.setSubtotal(BigDecimal.valueOf(100));

            given(discountService.findByCode("DATABASE")).willReturn(Optional.of(databaseDiscount));

            // When
            Cart result = promotionService.applyPromoCode(cart, code);

            // Then
            assertThat(result.getDiscount()).isEqualByComparingTo(BigDecimal.valueOf(20.00));
            assertThat(result.getPromoCode()).isEqualTo("DATABASE");
            verify(discountService).findByCode("DATABASE");
        }

        @Test
        @DisplayName("Should throw CartException for invalid promo code")
        void shouldThrowCartExceptionForInvalidPromoCode() {
            // Given
            String invalidCode = "INVALID";
            given(discountService.findByCode("INVALID")).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> promotionService.applyPromoCode(cart, invalidCode))
                    .isInstanceOf(CartException.class)
                    .hasMessage("Invalid promo code");
        }

        @Test
        @DisplayName("Should throw CartException when discount is not applicable")
        void shouldThrowCartExceptionWhenDiscountIsNotApplicable() {
            // Given
            String code = "EXPIRED";
            Discount expiredDiscount = createExpiredDiscount("1", code);
            given(discountService.findByCode("EXPIRED")).willReturn(Optional.of(expiredDiscount));

            // When & Then
            assertThatThrownBy(() -> promotionService.applyPromoCode(cart, code))
                    .isInstanceOf(CartException.class)
                    .hasMessage("Promo expired");
        }
    }

    @Nested
    @DisplayName("recalculate Tests")
    class RecalculateTests {

        @Test
        @DisplayName("Should do nothing when cart has no promo code")
        void shouldDoNothingWhenCartHasNoPromoCode() {
            // Given
            cart.setPromoCode(null);

            // When
            promotionService.recalculate(cart);

            // Then
            verifyNoInteractions(discountService);
        }

        @Test
        @DisplayName("Should do nothing when cart has blank promo code")
        void shouldDoNothingWhenCartHasBlankPromoCode() {
            // Given
            cart.setPromoCode("   ");

            // When
            promotionService.recalculate(cart);

            // Then
            verifyNoInteractions(discountService);
        }

        @Test
        @DisplayName("Should recalculate valid promo code")
        void shouldRecalculateValidPromoCode() {
            // Given
            String code = "RECALC";
            Discount validDiscount = createPercentageDiscount("1", code, BigDecimal.valueOf(15));

            // Add items consistent with subtotal
            CartItem item1 = createCartItem(1L, 1L, BigDecimal.valueOf(80));
            CartItem item2 = createCartItem(2L, 2L, BigDecimal.valueOf(20));
            cart.setItems(List.of(item1, item2));
            cart.setSubtotal(BigDecimal.valueOf(100));
            cart.setPromoCode("OLD_CODE"); // Cart with old promo code
            cart.setDiscount(BigDecimal.valueOf(5)); // Old discount

            given(discountService.findByCode("RECALC")).willReturn(Optional.of(validDiscount));

            // When
            Cart result = promotionService.applyPromoCode(cart, code);

            // Then
            assertThat(result.getDiscount()).isEqualByComparingTo(BigDecimal.valueOf(15.00)); // 15% of 100
            assertThat(result.getPromoCode()).isEqualTo("RECALC");
        }

        @Test
        @DisplayName("Should clear promo when recalculation fails")
        void shouldClearPromoWhenRecalculationFails() {
            // Given
            cart.setPromoCode("INVALID");
            cart.setDiscount(BigDecimal.valueOf(10));
            given(discountService.findByCode("INVALID")).willReturn(Optional.empty());

            // When
            promotionService.recalculate(cart);

            // Then
            assertThat(cart.getPromoCode()).isNull();
            assertThat(cart.getDiscount()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("Discount Validation Tests")
    class DiscountValidationTests {

        @Test
        @DisplayName("Should throw CartException when discount is expired")
        void shouldThrowCartExceptionWhenDiscountIsExpired() {
            // Given
            String code = "EXPIRED";
            Discount expiredDiscount = createExpiredDiscount("1", code);

            // Add items for consistency
            CartItem item = createCartItem(1L, 1L, BigDecimal.valueOf(50));
            cart.setItems(List.of(item));
            cart.setSubtotal(BigDecimal.valueOf(50));

            given(discountService.findByCode("EXPIRED")).willReturn(Optional.of(expiredDiscount));

            // When & Then
            assertThatThrownBy(() -> promotionService.applyPromoCode(cart, code))
                    .isInstanceOf(CartException.class)
                    .hasMessage("Promo expired");
        }

        @Test
        @DisplayName("Should throw CartException when discount is not yet active")
        void shouldThrowCartExceptionWhenDiscountIsNotYetActive() {
            // Given
            String code = "FUTURE";
            Discount futureDiscount = createFutureDiscount("1", code);

            // Add items for consistency
            CartItem item = createCartItem(1L, 1L, BigDecimal.valueOf(50));
            cart.setItems(List.of(item));
            cart.setSubtotal(BigDecimal.valueOf(50));

            given(discountService.findByCode("FUTURE")).willReturn(Optional.of(futureDiscount));

            // When & Then
            assertThatThrownBy(() -> promotionService.applyPromoCode(cart, code))
                    .isInstanceOf(CartException.class)
                    .hasMessage("Promo not yet active");
        }

        @Test
        @DisplayName("Should throw CartException when discount usage limit is reached")
        void shouldThrowCartExceptionWhenDiscountUsageLimitIsReached() {
            // Given
            String code = "LIMITED";
            Discount limitedDiscount = createLimitedDiscount("2", code);

            // Add items for consistency
            CartItem item = createCartItem(1L, 1L, BigDecimal.valueOf(50));
            cart.setItems(List.of(item));
            cart.setSubtotal(BigDecimal.valueOf(50));

            given(discountService.findByCode("LIMITED")).willReturn(Optional.of(limitedDiscount));

            // When & Then
            assertThatThrownBy(() -> promotionService.applyPromoCode(cart, code))
                    .isInstanceOf(CartException.class)
                    .hasMessage("Promo usage limit reached");
        }

        @Test
        @DisplayName("Should apply percentage discount correctly")
        void shouldApplyPercentageDiscountCorrectly() {
            // Given
            String code = "PERCENT25";
            Discount percentageDiscount = createPercentageDiscount("1", code, BigDecimal.valueOf(25));

            // Add items consistent with subtotal
            CartItem item1 = createCartItem(1L, 1L, BigDecimal.valueOf(60));
            CartItem item2 = createCartItem(2L, 2L, BigDecimal.valueOf(40));
            cart.setItems(List.of(item1, item2));
            cart.setSubtotal(BigDecimal.valueOf(100));

            given(discountService.findByCode("PERCENT25")).willReturn(Optional.of(percentageDiscount));

            // When
            Cart result = promotionService.applyPromoCode(cart, code);

            // Then
            assertThat(result.getDiscount()).isEqualByComparingTo(BigDecimal.valueOf(25.00));
            assertThat(result.getPromoCode()).isEqualTo("PERCENT25");
        }

        @Test
        @DisplayName("Should apply fixed amount discount correctly")
        void shouldApplyFixedAmountDiscountCorrectly() {
            // Given
            String code = "FIXED15";
            Discount fixedDiscount = createFixedAmountDiscount("1", code, BigDecimal.valueOf(15));

            // Add items consistent with subtotal
            CartItem item1 = createCartItem(1L, 1L, BigDecimal.valueOf(60));
            CartItem item2 = createCartItem(2L, 2L, BigDecimal.valueOf(40));
            cart.setItems(List.of(item1, item2));
            cart.setSubtotal(BigDecimal.valueOf(100));

            given(discountService.findByCode("FIXED15")).willReturn(Optional.of(fixedDiscount));

            // When
            Cart result = promotionService.applyPromoCode(cart, code);

            // Then
            assertThat(result.getDiscount()).isEqualByComparingTo(BigDecimal.valueOf(15.00));
            assertThat(result.getPromoCode()).isEqualTo("FIXED15");
        }

        @Test
        @DisplayName("Should cap discount at eligible amount")
        void shouldCapDiscountAtEligibleAmount() {
            // Given
            String code = "HUGE";
            Discount hugeDiscount = createFixedAmountDiscount("1", code, BigDecimal.valueOf(200));

            // Cart with amount lower than discount
            CartItem item = createCartItem(1L, 1L, BigDecimal.valueOf(50));
            cart.setItems(List.of(item));
            cart.setSubtotal(BigDecimal.valueOf(50));

            given(discountService.findByCode("HUGE")).willReturn(Optional.of(hugeDiscount));

            // When
            Cart result = promotionService.applyPromoCode(cart, code);

            // Then
            assertThat(result.getDiscount()).isEqualByComparingTo(BigDecimal.valueOf(50.00));
            assertThat(result.getPromoCode()).isEqualTo("HUGE");
        }

        @Test
        @DisplayName("Should throw CartException when discount amount is zero")
        void shouldThrowCartExceptionWhenDiscountAmountIsZero() {
            // Given
            String code = "ZERO";
            Discount zeroDiscount = createFixedAmountDiscount("1", code, BigDecimal.ZERO);

            // Even with items, discount will be zero
            CartItem item = createCartItem(1L, 1L, BigDecimal.valueOf(50));
            cart.setItems(List.of(item));
            cart.setSubtotal(BigDecimal.valueOf(50));

            given(discountService.findByCode("ZERO")).willReturn(Optional.of(zeroDiscount));

            // When & Then
            assertThatThrownBy(() -> promotionService.applyPromoCode(cart, code))
                    .isInstanceOf(CartException.class)
                    .hasMessage("Promo not applicable");
        }

        @Test
        @DisplayName("Should throw CartException when cart has zero subtotal")
        void shouldThrowCartExceptionWhenCartHasZeroSubtotal() {
            // Given
            String code = "VALID";
            Discount validDiscount = createPercentageDiscount("1", code, BigDecimal.valueOf(10));

            // Cart with free items
            CartItem freeItem = createCartItem(1L, 1L, BigDecimal.ZERO);
            cart.setItems(List.of(freeItem));
            cart.setSubtotal(BigDecimal.ZERO);

            given(discountService.findByCode("VALID")).willReturn(Optional.of(validDiscount));

            // When & Then
            assertThatThrownBy(() -> promotionService.applyPromoCode(cart, code))
                    .isInstanceOf(CartException.class)
                    .hasMessage("Promo not applicable");
        }

        @Test
        @DisplayName("Should throw CartException when cart is empty")
        void shouldThrowCartExceptionWhenCartIsEmpty() {
            // Given
            String code = "VALID";
            Discount validDiscount = createPercentageDiscount("1", code, BigDecimal.valueOf(10));

            // Explicitly empty cart
            cart.setItems(List.of());
            cart.setSubtotal(BigDecimal.ZERO);

            given(discountService.findByCode("VALID")).willReturn(Optional.of(validDiscount));

            // When & Then
            assertThatThrownBy(() -> promotionService.applyPromoCode(cart, code))
                    .isInstanceOf(CartException.class)
                    .hasMessage("Promo not applicable");
        }
    }

    @Nested
    @DisplayName("Discount Criteria Tests")
    class DiscountCriteriaTests {

        @Test
        @DisplayName("Should apply discount only to eligible items by event")
        void shouldApplyDiscountOnlyToEligibleItemsByEvent() {
            // Given
            String code = "EVENT_SPECIFIC";
            Long targetEventId = 1L;
            Discount eventDiscount = createEventSpecificDiscount("1", code, targetEventId);

            CartItem eligibleItem = createCartItem(1L, targetEventId, BigDecimal.valueOf(50));
            CartItem ineligibleItem = createCartItem(2L, 2L, BigDecimal.valueOf(30));
            cart.setItems(List.of(eligibleItem, ineligibleItem));

            given(discountService.findByCode("EVENT_SPECIFIC")).willReturn(Optional.of(eventDiscount));

            // When
            Cart result = promotionService.applyPromoCode(cart, code);

            // Then
            // Should only apply to the eligible item (50 * 10% = 5)
            assertThat(result.getDiscount()).isEqualByComparingTo(BigDecimal.valueOf(5.00));
        }

        @Test
        @DisplayName("Should apply discount to all items when no restrictions")
        void shouldApplyDiscountToAllItemsWhenNoRestrictions() {
            // Given
            String code = "GENERAL";
            Discount generalDiscount = createPercentageDiscount("1", code, BigDecimal.valueOf(10));
            CartItem item1 = createCartItem(1L, 1L, BigDecimal.valueOf(60));
            CartItem item2 = createCartItem(2L, 2L, BigDecimal.valueOf(40));
            cart.setItems(List.of(item1, item2));
            cart.setSubtotal(BigDecimal.valueOf(100)); // 60 + 40

            given(discountService.findByCode("GENERAL")).willReturn(Optional.of(generalDiscount));

            // When
            Cart result = promotionService.applyPromoCode(cart, code);

            // Then
            assertThat(result.getDiscount()).isEqualByComparingTo(BigDecimal.valueOf(10.00));
        }

        @Test
        @DisplayName("Should throw CartException when no items are eligible")
        void shouldThrowCartExceptionWhenNoItemsAreEligible() {
            // Given
            String code = "SPECIFIC";
            Discount specificDiscount = createEventSpecificDiscount("1", code, 999L);
            CartItem item = createCartItem(1L, 1L, BigDecimal.valueOf(50));
            cart.setItems(List.of(item));
            cart.setSubtotal(BigDecimal.ZERO); // No fallback

            given(discountService.findByCode("SPECIFIC")).willReturn(Optional.of(specificDiscount));

            // When & Then
            assertThatThrownBy(() -> promotionService.applyPromoCode(cart, code))
                    .isInstanceOf(CartException.class)
                    .hasMessage("Promo not applicable");
        }
    }

    // Helper methods
    private Cart createCart() {
        Cart newCart = new Cart();
        newCart.setSubtotal(BigDecimal.valueOf(100));
        newCart.setFees(BigDecimal.valueOf(5));
        newCart.setDiscount(BigDecimal.ZERO);
        newCart.setTotal(BigDecimal.valueOf(105));
        return newCart;
    }

    private CartItem createCartItem(Long ticketTypeId, Long eventId, BigDecimal totalPrice) {
        CartItem item = new CartItem();
        item.setTicketTypeId(ticketTypeId);
        item.setEventId(eventId);
        item.setTotalPrice(totalPrice);
        item.setQuantity(1);
        item.setUnitPrice(totalPrice);
        return item;
    }

    private Discount createPercentageDiscount(String id, String code, BigDecimal percentOff) {
        Discount discount = new Discount();
        discount.setId(id);
        discount.setCode(code);
        discount.setType(DiscountType.PERCENTAGE);
        discount.setPercentOff(percentOff);
        discount.setStartDate(LocalDateTime.now().minusDays(1));
        discount.setEndDate(LocalDateTime.now().plusDays(1));
        return discount;
    }

    private Discount createFixedAmountDiscount(String id, String code, BigDecimal amountOff) {
        Discount discount = new Discount();
        discount.setId(id);
        discount.setCode(code);
        discount.setType(DiscountType.FIXED_AMOUNT);
        discount.setAmountOff(amountOff);
        discount.setStartDate(LocalDateTime.now().minusDays(1));
        discount.setEndDate(LocalDateTime.now().plusDays(1));
        return discount;
    }

    private Discount createExpiredDiscount(String id, String code) {
        Discount discount = new Discount();
        discount.setId(id);
        discount.setCode(code);
        discount.setType(DiscountType.PERCENTAGE);
        discount.setPercentOff(BigDecimal.valueOf(10));
        discount.setStartDate(LocalDateTime.now().minusDays(2));
        discount.setEndDate(LocalDateTime.now().minusDays(1)); // Expired
        return discount;
    }

    private Discount createFutureDiscount(String id, String code) {
        Discount discount = new Discount();
        discount.setId(id);
        discount.setCode(code);
        discount.setType(DiscountType.PERCENTAGE);
        discount.setPercentOff(BigDecimal.valueOf(10));
        discount.setStartDate(LocalDateTime.now().plusDays(1)); // Future
        discount.setEndDate(LocalDateTime.now().plusDays(2));
        return discount;
    }

    private Discount createLimitedDiscount(String id, String code) {
        Discount discount = new Discount();
        discount.setId(id);
        discount.setCode(code);
        discount.setType(DiscountType.PERCENTAGE);
        discount.setPercentOff(BigDecimal.valueOf(10));
        discount.setQuantityAvailable(10);
        discount.setQuantitySold(10); // Limit reached
        return discount;
    }

    private Discount createEventSpecificDiscount(String id, String code, Long eventId) {
        Discount discount = createPercentageDiscount(id, code, BigDecimal.valueOf(10));
        Event event = new Event();
        event.setId(eventId);
        discount.setEvent(event);
        return discount;
    }

}

