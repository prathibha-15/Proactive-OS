package com.proactiveos.integrations.dto;

import java.time.Instant;
import java.util.List;

import com.proactiveos.events.entity.LifeEventType;
import com.proactiveos.integrations.entity.ExternalProviderId;

public record IntegrationProviderStatus(
        ExternalProviderId provider,
        String displayName,
        boolean developmentOnly,
        List<LifeEventType> supportedEventTypes,
        Instant lastSyncedAt
) {
}