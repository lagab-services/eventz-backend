package com.lagab.eventz.app.domain.event.exception;

import com.lagab.eventz.app.common.exception.ValidationException;

public class DuplicateReviewException extends ValidationException {
    public DuplicateReviewException(String message) {
        super(message);
    }

}
