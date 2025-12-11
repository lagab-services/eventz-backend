package com.lagab.eventz.app.domain.order.exception;

import com.lagab.eventz.app.common.exception.BusinessException;

public class OrderException extends BusinessException {
    public OrderException(String message) {
        super(message);
    }
}
