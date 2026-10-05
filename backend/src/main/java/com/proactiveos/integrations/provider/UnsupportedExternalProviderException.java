package com.proactiveos.integrations.provider;

import com.proactiveos.integrations.entity.ExternalProviderId;

public class UnsupportedExternalProviderException extends RuntimeException {

    public UnsupportedExternalProviderException(ExternalProviderId providerId) {
        super("External provider is not available: " + providerId);
    }
}