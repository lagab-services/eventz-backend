package com.lagab.eventz.app.domain.event.exception;

import com.lagab.eventz.app.common.exception.ResourceNotFoundException;

public class AddressNotFoundException extends ResourceNotFoundException {
    public AddressNotFoundException(String message) {
        super(message);
    }

}
