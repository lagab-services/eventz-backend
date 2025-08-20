package com.lagab.eventz.app.domain.cart.dto;

public enum CartErrorCode {
    // Availability errors
    TICKET_NOT_ON_SALE("ticket.not_on_sale"),
    TICKET_OUT_OF_STOCK("ticket.out_of_stock"),
    TICKET_NOT_FOUND("ticket.not_found"),

    // Quantity errors
    QUANTITY_BELOW_MINIMUM("quantity.below_minimum"),
    QUANTITY_ABOVE_MAXIMUM("quantity.above_maximum"),

    // Cart errors
    CART_EMPTY("cart.empty"),
    MIXED_EVENTS("cart.mixed_events"),

    // Validation errors
    VALIDATION_ERROR("validation.error");

    private final String code;

    CartErrorCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
