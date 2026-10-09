package com.proactiveos.integrations.provider;

import java.util.List;
import java.util.Set;

import com.proactiveos.events.entity.LifeEventType;
import com.proactiveos.integrations.dto.ExternalActivityRecord;
import com.proactiveos.integrations.entity.ExternalProviderId;
import org.springframework.stereotype.Component;

@Component
public class HealthConnectExternalDataProvider implements ExternalDataProvider {

    @Override
    public ExternalProviderId providerId() {
        return ExternalProviderId.HEALTH_CONNECT;
    }

    @Override
    public String displayName() {
        return "Health Connect (Android companion required)";
    }

    @Override
    public boolean developmentOnly() {
        return false;
    }

    @Override
    public boolean clientUploadRequired() {
        return true;
    }

    @Override
    public Set<LifeEventType> supportedEventTypes() {
        return Set.of(LifeEventType.STEPS, LifeEventType.SLEEP);
    }

    @Override
    public List<ExternalActivityRecord> fetchRecords(Long ownerId) {
        throw new IllegalStateException("Health Connect records must be uploaded by the Android companion.");
    }
}