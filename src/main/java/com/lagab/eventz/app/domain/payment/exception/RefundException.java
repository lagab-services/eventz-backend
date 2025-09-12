package com.lagab.eventz.app.domain.payment.exception;

import com.lagab.eventz.app.common.exception.BusinessException;

public class RefundException extends BusinessException {
    public RefundException(String message) {
        super(message);
    }
}
