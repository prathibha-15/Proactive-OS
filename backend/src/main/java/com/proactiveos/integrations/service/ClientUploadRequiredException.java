package com.proactiveos.integrations.service;

import com.proactiveos.integrations.entity.ExternalProviderId;

public class ClientUploadRequiredException extends RuntimeException {

    public ClientUploadRequiredException(ExternalProviderId provider) {
        super(provider + " requires records uploaded by the Android companion.");
    }
}