package com.lagab.eventz.app.domain.payment.exception;

import com.lagab.eventz.app.common.exception.BusinessException;

public class PaymentException extends BusinessException {
    public PaymentException(String message) {
        super(message);
    }
}
