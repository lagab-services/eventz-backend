package com.lagab.eventz.app.domain.event.exception;

import com.lagab.eventz.app.common.exception.ResourceNotFoundException;

public class UserNotFoundException extends ResourceNotFoundException {
    public UserNotFoundException(String message) {
        super(message);
    }

}
