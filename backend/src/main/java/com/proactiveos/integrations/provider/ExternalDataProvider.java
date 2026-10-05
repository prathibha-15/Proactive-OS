package com.proactiveos.integrations.provider;

import java.util.List;
import java.util.Set;

import com.proactiveos.events.entity.LifeEventType;
import com.proactiveos.integrations.dto.ExternalActivityRecord;
import com.proactiveos.integrations.entity.ExternalProviderId;

public interface ExternalDataProvider {

    ExternalProviderId providerId();

    String displayName();

    boolean developmentOnly();

    Set<LifeEventType> supportedEventTypes();

    List<ExternalActivityRecord> fetchRecords(Long ownerId);
}