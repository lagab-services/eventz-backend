package com.lagab.eventz.app.interfaces.web.cart;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lagab.eventz.app.domain.auth.service.ApiKeyService;
import com.lagab.eventz.app.domain.auth.service.JwtService;
import com.lagab.eventz.app.domain.cart.dto.CartMessage;
import com.lagab.eventz.app.domain.cart.dto.CartResponse;
import com.lagab.eventz.app.domain.cart.exception.CartException;
import com.lagab.eventz.app.domain.cart.model.Cart;
import com.lagab.eventz.app.domain.cart.model.CartItem;
import com.lagab.eventz.app.domain.cart.model.CartValidationResult;
import com.lagab.eventz.app.domain.cart.service.CartService;
import com.lagab.eventz.app.domain.user.repository.UserRepository;
import com.lagab.eventz.app.interfaces.web.cart.mapper.CartMapper;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("CartController Integration Tests")
class CartControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ApiKeyService apiKeyService;

    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private CartMapper cartMapper;

    private MockHttpSession session;
    private Cart testCart;
    private CartItem testCartItem;
    private CartResponse testCartResponse;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
        session.setAttribute("JSESSIONID", "test-session-123");

        setupTestData();
        setupMockBehavior();
    }

    private void setupTestData() {
        // Create test cart item
        testCartItem = new CartItem();
        testCartItem.setTicketTypeId(1L);
        testCartItem.setTicketTypeName("VIP Ticket");
        testCartItem.setEventTitle("Test Event");
        testCartItem.setUnitPrice(new BigDecimal("100.00"));
        testCartItem.setQuantity(2);
        testCartItem.setTotalPrice(new BigDecimal("200.00"));
        testCartItem.setAvailableQuantity(10);

        // Create test cart
        testCart = new Cart();
        testCart.setSessionId("test-session-123");
        testCart.setUserId(null);
        testCart.getItems().add(testCartItem);
        testCart.setSubtotal(new BigDecimal("200.00"));
        testCart.setFees(BigDecimal.ZERO);
        testCart.setDiscount(BigDecimal.ZERO);
        testCart.setTotal(new BigDecimal("200.00"));
        testCart.setUpdatedAt(LocalDateTime.now());

        // Create test response
        CartResponse.CartItemResponse itemResponse = new CartResponse.CartItemResponse(
                1L, "VIP Ticket", "Test Event", new BigDecimal("100.00"),
                2, new BigDecimal("200.00"), 10, true
        );

        testCartResponse = new CartResponse(
                List.of(itemResponse),
                new BigDecimal("200.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("200.00"),
                2,
                LocalDateTime.now(),
                null,
                true,
                new ArrayList<>(),
                new ArrayList<>()
        );
    }

    private void setupMockBehavior() {
        // By default, map cart and validation result using the mapper mock to a prepared response
        when(cartMapper.toCartResponse(any(Cart.class))).thenReturn(testCartResponse);

        // Validation mapping - craft a specific response via mapper for validation results
        when(cartMapper.toCartResponse(any(CartValidationResult.class))).thenAnswer(invocation -> {
            CartValidationResult vr = invocation.getArgument(0);
            // Map using the same shape as testCartResponse, but reflect validity and messages size
            return new CartResponse(
                    testCartResponse.items(),
                    testCartResponse.subtotal(),
                    testCartResponse.fees(),
                    testCartResponse.discount(),
                    testCartResponse.total(),
                    testCartResponse.totalItems(),
                    testCartResponse.updatedAt(),
                    testCartResponse.promoCode(),
                    vr.isValid(),
                    vr.getWarnings(),
                    vr.getErrors()
            );
        });
    }

    @Nested
    @DisplayName("GET /api/cart")
    class GetCart {
        @Test
        @WithMockUser
        void should_return_current_cart() throws Exception {
            when(cartService.getCart(anyString(), any())).thenReturn(testCart);

            mockMvc.perform(get("/api/cart").session(session))
                   .andDo(print())
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.items", hasSize(1)))
                   .andExpect(jsonPath("$.items[0].ticketTypeId", is(1)))
                   .andExpect(jsonPath("$.isValid", is(true)));

            verify(cartService).getCart(anyString(), any());
            verify(cartMapper).toCartResponse(any(Cart.class));
        }
    }

    @Nested
    @DisplayName("POST /api/cart/items")
    class AddToCart {
        @Test
        @WithMockUser
        void should_validate_ticketTypeId_and_return_400_when_missing_or_invalid() throws Exception {
            mockMvc.perform(post("/api/cart/items")
                           .param("ticketTypeId", "0")
                           .param("quantity", "2")
                           .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                           .session(session)
                           .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                   .andExpect(status().isBadRequest());

            verify(cartService, never()).addToCart(anyString(), any(), anyLong(), any());
        }

        @Test
        @WithMockUser
        void should_validate_quantity_and_return_400_when_invalid() throws Exception {
            mockMvc.perform(post("/api/cart/items")
                           .param("ticketTypeId", "1")
                           .param("quantity", "0")
                           .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                           .session(session)
                           .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                   .andExpect(status().isBadRequest());

            verify(cartService, never()).addToCart(anyString(), any(), anyLong(), any());
        }

        @Test
        @WithMockUser
        void should_add_item_and_return_cart() throws Exception {
            when(cartService.addToCart(anyString(), any(), eq(1L), eq(2))).thenReturn(testCart);

            mockMvc.perform(post("/api/cart/items")
                           .param("ticketTypeId", "1")
                           .param("quantity", "2")
                           .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                           .session(session)
                           .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                   .andDo(print())
                   .andExpect(status().isOk())
                   // Response is built by controller's mapToCartResponse
                   .andExpect(jsonPath("$.items", hasSize(1)))
                   .andExpect(jsonPath("$.items[0].ticketTypeId", is(1)))
                   .andExpect(jsonPath("$.items[0].isAvailable", is(true)))
                   .andExpect(jsonPath("$.totalItems", is(2)))
                   .andExpect(jsonPath("$.isValid", is(true)));

            verify(cartService).addToCart(anyString(), any(), eq(1L), eq(2));
        }

        @Test
        @WithMockUser
        void should_return_400_with_mapped_error_on_business_exception() throws Exception {
            when(cartService.addToCart(anyString(), any(), eq(1L), eq(2)))
                    .thenThrow(new CartException("Stock épuisé"));

            // Prepare mapper error response
            CartMessage error = CartMessage.error(com.lagab.eventz.app.domain.cart.dto.CartErrorCode.TICKET_OUT_OF_STOCK, "Stock épuisé");
            CartResponse errorResponse = new CartResponse(
                    List.of(), null, null, null, null, 0, null, null, false, List.of(), List.of(error)
            );
            when(cartMapper.createErrorResponse(any(CartMessage.class))).thenReturn(errorResponse);

            mockMvc.perform(post("/api/cart/items")
                           .param("ticketTypeId", "1")
                           .param("quantity", "2")
                           .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                           .session(session)
                           .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                   .andExpect(status().isBadRequest())
                   .andExpect(jsonPath("$.errors", hasSize(1)))
                   .andExpect(jsonPath("$.errors[0].code", is("ticket.out_of_stock")));
        }
    }

    @Nested
    @DisplayName("PUT /api/cart/items/{ticketTypeId}")
    class UpdateCartItem {
        @Test
        @WithMockUser
        void should_validate_quantity_non_negative() throws Exception {
            mockMvc.perform(put("/api/cart/items/{ticketTypeId}", 1)
                           .param("quantity", "-1")
                           .session(session)
                           .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                   .andExpect(status().isBadRequest());

            verify(cartService, never()).updateCartItem(anyString(), any(), anyLong(), any());
        }

        @Test
        @WithMockUser
        void should_update_item_and_return_cart() throws Exception {
            when(cartService.updateCartItem(anyString(), any(), eq(1L), eq(3))).thenReturn(testCart);

            mockMvc.perform(put("/api/cart/items/{ticketTypeId}", 1)
                           .param("quantity", "3")
                           .session(session)
                           .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.items", hasSize(1)))
                   .andExpect(jsonPath("$.items[0].ticketTypeId", is(1)))
                   .andExpect(jsonPath("$.isValid", is(true)));
        }
    }

    @Nested
    @DisplayName("DELETE /api/cart/items/{ticketTypeId}")
    class RemoveFromCart {
        @Test
        @WithMockUser
        void should_remove_item_and_return_cart() throws Exception {
            when(cartService.removeFromCart(anyString(), any(), eq(1L))).thenReturn(testCart);

            mockMvc.perform(delete("/api/cart/items/{ticketTypeId}", 1)
                           .session(session)
                           .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.items", hasSize(1)))
                   .andExpect(jsonPath("$.items[0].ticketTypeId", is(1)));
        }
    }

    @Nested
    @DisplayName("DELETE /api/cart")
    class ClearCart {
        @Test
        @WithMockUser
        void should_clear_cart_and_return_204() throws Exception {
            doNothing().when(cartService).clearCart(anyString(), any());

            mockMvc.perform(delete("/api/cart")
                           .session(session)
                           .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                   .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("POST /api/cart/validate")
    class ValidateCart {
        @Test
        @WithMockUser
        void should_validate_and_return_result() throws Exception {
            CartValidationResult result = new CartValidationResult(testCart);
            // Add a warning to ensure it's surfaced
            result.addWarning(com.lagab.eventz.app.domain.cart.dto.CartWarningCode.PRICE_INCREASED, "Price changed");
            when(cartService.validateAndRefreshCart(anyString(), any())).thenReturn(result);

            mockMvc.perform(post("/api/cart/validate")
                           .session(session)
                           .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.warnings", hasSize(1)))
                   .andExpect(jsonPath("$.isValid", is(true)));

            verify(cartMapper).toCartResponse(any(CartValidationResult.class));
        }
    }

    @Nested
    @DisplayName("POST /api/cart/promo")
    class PromoCode {
        @Test
        @WithMockUser
        void should_apply_promo_and_return_cart() throws Exception {
            when(cartService.applyPromoCode(anyString(), any(), eq("PROMO10"))).thenReturn(testCart);

            mockMvc.perform(post("/api/cart/promo").param("code", "PROMO10")
                                                   .session(session)
                                                   .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.items", hasSize(1)))
                   .andExpect(jsonPath("$.isValid", is(true)));
        }

        @Test
        @WithMockUser
        void should_return_400_when_promo_invalid() throws Exception {
            when(cartService.applyPromoCode(anyString(), any(), eq("BAD")))
                    .thenThrow(new CartException("invalid code"));

            CartMessage error = CartMessage.error(com.lagab.eventz.app.domain.cart.dto.CartErrorCode.VALIDATION_ERROR, "invalid code");
            CartResponse errorResponse = new CartResponse(
                    List.of(), null, null, null, null, 0, null, null, false, List.of(), List.of(error)
            );
            when(cartMapper.createErrorResponse(any(CartMessage.class))).thenReturn(errorResponse);

            mockMvc.perform(post("/api/cart/promo").param("code", "BAD")
                                                   .session(session)
                                                   .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                   .andExpect(status().isBadRequest())
                   .andExpect(jsonPath("$.errors", hasSize(1)))
                   .andExpect(jsonPath("$.errors[0].code", is("validation.error")));
        }
    }

}
