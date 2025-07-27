package com.lagab.eventz.app.domain.event.exception;

import com.lagab.eventz.app.common.exception.ResourceNotFoundException;

public class EventNotFoundException extends ResourceNotFoundException {
    public EventNotFoundException(String message) {
        super(message);
    }

}
