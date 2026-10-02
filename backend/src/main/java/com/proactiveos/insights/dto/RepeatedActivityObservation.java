package com.proactiveos.insights.dto;

import com.proactiveos.events.entity.LifeEventType;

public record RepeatedActivityObservation(
        LifeEventType type,
        String label,
        long distinctDays,
        long eventCount
) {
}
