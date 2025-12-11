package com.lagab.eventz.app.domain.cart.dto;

public enum CartWarningCode {
    // Price warnings
    PRICE_INCREASED("price.increased"),
    PRICE_DECREASED("price.decreased"),

    // Quantity warnings
    QUANTITY_REDUCED_STOCK("quantity.reduced.stock"),
    QUANTITY_REDUCED_LIMIT("quantity.reduced.limit"),
    LOW_STOCK("stock.low"),

    // General warnings
    CART_UPDATED("cart.updated");

    private final String code;

    CartWarningCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
