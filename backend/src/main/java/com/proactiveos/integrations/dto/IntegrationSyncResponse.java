package com.proactiveos.integrations.dto;

import java.time.Instant;

import com.proactiveos.integrations.entity.ExternalProviderId;

public record IntegrationSyncResponse(
        ExternalProviderId provider,
        int eventsFetched,
        int eventsCreated,
        int eventsUpdated,
        int eventsSkipped,
        Instant lastSyncedAt
) {
}