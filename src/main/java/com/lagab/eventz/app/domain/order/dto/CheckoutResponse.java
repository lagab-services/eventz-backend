package com.lagab.eventz.app.domain.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutResponse {
    private String checkoutUrl;
    private String orderId;
    private String sessionId;
    private Long expiresAt; // timestamp
}
