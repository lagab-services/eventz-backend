package com.lagab.eventz.app.domain.cart.model;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {
    private Long ticketTypeId;
    private String ticketTypeName;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal totalPrice;
    private Long eventId;
    private String eventTitle;

    // datas for validation
    private Integer availableQuantity;
    private Integer maxQuantityPerOrder;
    private Integer minQuantityPerOrder;

    public void calculateTotalPrice() {
        this.totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
