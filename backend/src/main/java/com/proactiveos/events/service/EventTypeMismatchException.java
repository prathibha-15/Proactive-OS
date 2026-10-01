package com.proactiveos.events.service;

import com.proactiveos.events.entity.LifeEventType;

public class EventTypeMismatchException extends RuntimeException {

    public EventTypeMismatchException(Long eventId, LifeEventType existingType, LifeEventType requestedType) {
        super("Life event %d is of type %s and cannot be changed to %s.".formatted(eventId, existingType, requestedType));
    }
}
