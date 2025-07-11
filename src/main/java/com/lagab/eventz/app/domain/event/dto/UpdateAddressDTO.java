package com.lagab.eventz.app.domain.event.dto;

import java.math.BigDecimal;

public record UpdateAddressDTO(
        String name,
        String address1,
        String address2,
        String city,
        String state,
        String country,
        String zipCode,
        BigDecimal longitude,
        BigDecimal latitude,
        Boolean isOnline,
        String onlineUrl
) {
}
