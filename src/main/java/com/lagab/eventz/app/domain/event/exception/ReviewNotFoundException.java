package com.lagab.eventz.app.domain.event.exception;

import com.lagab.eventz.app.common.exception.ResourceNotFoundException;

public class ReviewNotFoundException extends ResourceNotFoundException {
    public ReviewNotFoundException(String message) {
        super(message);
    }

}
