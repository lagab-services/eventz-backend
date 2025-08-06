package com.lagab.eventz.app.domain.event.projection;

import java.math.BigDecimal;

public interface TicketTypeStatsProjection {
    Long getTotalTicketTypes();

    Long getActiveTicketTypes();

    Long getSoldOutTicketTypes();

    Integer getTotalCapacity();

    Integer getTotalSold();

    Integer getTotalRemaining();

    BigDecimal getTotalRevenue();

    Double getAveragePrice();
}
