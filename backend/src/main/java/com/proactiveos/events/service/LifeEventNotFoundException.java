package com.proactiveos.events.service;

public class LifeEventNotFoundException extends RuntimeException {

    public LifeEventNotFoundException(Long eventId) {
        super("Life event %d was not found.".formatted(eventId));
    }
}
