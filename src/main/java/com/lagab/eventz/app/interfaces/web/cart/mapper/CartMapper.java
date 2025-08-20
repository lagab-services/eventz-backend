package com.lagab.eventz.app.interfaces.web.cart.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.lagab.eventz.app.domain.cart.dto.CartMessage;
import com.lagab.eventz.app.domain.cart.dto.CartResponse;
import com.lagab.eventz.app.domain.cart.model.Cart;
import com.lagab.eventz.app.domain.cart.model.CartItem;
import com.lagab.eventz.app.domain.cart.model.CartValidationResult;

@Mapper(componentModel = "spring")
public interface CartMapper {

    // Mapping from simple Cart (without validation)
    @Mapping(target = "isValid", source = ".", qualifiedByName = "validateCart")
    @Mapping(target = "warnings", expression = "java(java.util.List.of())")
    @Mapping(target = "errors", expression = "java(java.util.List.of())")
    CartResponse toCartResponse(Cart cart);

    // Mapping from CartValidationResult (with warnings/errors separation)
    @Mapping(target = "items", source = "cart.items")
    @Mapping(target = "subtotal", source = "cart.subtotal")
    @Mapping(target = "fees", source = "cart.fees")
    @Mapping(target = "total", source = "cart.total")
    @Mapping(target = "totalItems", source = "cart.totalItems")
    @Mapping(target = "updatedAt", source = "cart.updatedAt")
    @Mapping(target = "isValid", source = "valid")
    @Mapping(target = "warnings", source = "warnings")
    @Mapping(target = "errors", source = "errors")
    CartResponse toCartResponse(CartValidationResult validationResult);

    @Mapping(target = "isAvailable", source = ".", qualifiedByName = "isItemAvailable")
    CartResponse.CartItemResponse toCartItemResponse(CartItem cartItem);

    @Named("validateCart")
    default boolean validateCart(Cart cart) {
        return !cart.isEmpty(); // Default implementation
    }

    @Named("isItemAvailable")
    default boolean isItemAvailable(CartItem cartItem) {
        return cartItem.getAvailableQuantity() != null && cartItem.getAvailableQuantity() > 0;
    }

    // Utility methods to create error responses
    @Named("createErrorResponse")
    default CartResponse createErrorResponse(CartMessage errorMessage) {
        return new CartResponse(
                List.of(),
                null,
                null,
                null,
                0,
                null,
                false,
                List.of(),
                List.of(errorMessage)
        );
    }

    @Named("createErrorResponseWithLists")
    default CartResponse createErrorResponse(List<CartMessage> warnings, List<CartMessage> errors) {
        return new CartResponse(
                List.of(),
                null,
                null,
                null,
                0,
                null,
                errors.isEmpty(),
                warnings,
                errors
        );
    }
}
