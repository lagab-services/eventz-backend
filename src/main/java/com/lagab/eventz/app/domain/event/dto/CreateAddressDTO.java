package com.lagab.eventz.app.domain.event.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;

public record CreateAddressDTO(
        String name,

        @NotBlank(message = "Address line 1 is required")
        String address1,

        String address2,

        @NotBlank(message = "City is required")
        String city,

        String state,

        @NotBlank(message = "Country is required")
        String country,

        String zipCode,
        BigDecimal longitude,
        BigDecimal latitude,
        Boolean isOnline,
        String onlineUrl
) {
}
