package com.proactiveos.integrations.provider;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import com.proactiveos.events.entity.LifeEventType;
import com.proactiveos.integrations.dto.ExternalActivityRecord;
import com.proactiveos.integrations.entity.ExternalProviderId;
import org.springframework.stereotype.Component;

@Component
public class MockExternalDataProvider implements ExternalDataProvider {

    private static final List<ExternalActivityRecord> RECORDS = List.of(
            new ExternalActivityRecord("steps-2026-10-03", LifeEventType.STEPS,
                    Instant.parse("2026-10-03T23:59:59Z"), null, 8247, null, null),
            new ExternalActivityRecord("workout-legs-2026-10-01", LifeEventType.WORKOUT,
                    Instant.parse("2026-10-01T18:00:00Z"), 40, null, "Leg workout", null),
            new ExternalActivityRecord("workout-legs-2026-10-02", LifeEventType.WORKOUT,
                    Instant.parse("2026-10-02T18:00:00Z"), 40, null, "Leg workout", null),
            new ExternalActivityRecord("workout-legs-2026-10-03", LifeEventType.WORKOUT,
                    Instant.parse("2026-10-03T18:00:00Z"), 40, null, "Leg workout", null),
            new ExternalActivityRecord("sleep-2026-10-03", LifeEventType.SLEEP,
                    Instant.parse("2026-10-03T00:30:00Z"), 480, null, null,
                    "Development sample sleep activity"));

    @Override
    public ExternalProviderId providerId() {
        return ExternalProviderId.MOCK;
    }

    @Override
    public String displayName() {
        return "Development mock provider";
    }

    @Override
    public boolean developmentOnly() {
        return true;
    }

    @Override
    public Set<LifeEventType> supportedEventTypes() {
        return Set.of(LifeEventType.STEPS, LifeEventType.WORKOUT, LifeEventType.SLEEP);
    }

    @Override
    public List<ExternalActivityRecord> fetchRecords(Long ownerId) {
        return RECORDS;
    }
}