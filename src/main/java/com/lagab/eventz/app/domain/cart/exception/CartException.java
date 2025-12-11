package com.lagab.eventz.app.domain.cart.exception;

import com.lagab.eventz.app.common.exception.BusinessException;

public class CartException extends BusinessException {
    public CartException(String message) {
        super(message);
    }
}
