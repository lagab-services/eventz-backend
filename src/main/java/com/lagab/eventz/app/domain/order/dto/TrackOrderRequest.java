package com.lagab.eventz.app.domain.order.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record TrackOrderRequest(
        @NotBlank String orderNumber,
        @NotBlank @Email String email
) {}
