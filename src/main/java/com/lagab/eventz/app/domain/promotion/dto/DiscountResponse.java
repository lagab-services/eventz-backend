package com.lagab.eventz.app.domain.promotion.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.lagab.eventz.app.domain.promotion.model.DiscountType;

public record DiscountResponse(
        String id,
        DiscountType type,
        String code,
        BigDecimal amount_off,
        BigDecimal percent_off,
        Long eventId,
        List<Long> ticket_type_ids,
        Integer quantity_available,
        Integer quantity_sold,
        LocalDateTime start_date,
        LocalDateTime end_date,
        Integer end_date_relative,
        Long ticket_category_id
) {}
