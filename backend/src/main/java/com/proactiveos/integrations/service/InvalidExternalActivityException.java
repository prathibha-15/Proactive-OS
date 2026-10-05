package com.proactiveos.integrations.service;

public class InvalidExternalActivityException extends RuntimeException {

    public InvalidExternalActivityException(String message) {
        super(message);
    }
}