package com.proactiveos.events.dto;

import java.time.Instant;

import com.proactiveos.events.entity.EventSource;
import com.proactiveos.events.entity.LifeEventType;
import com.proactiveos.events.entity.WaterUnit;

/**
 * Flat response mirroring {@link EventRequest}; fields not applicable to the event's type are null.
 */
public record EventResponse(
        Long id,
        LifeEventType type,
        EventSource source,
        Long journalEntryId,
        Instant eventTime,
        Double confidence,
        String subject,
        Integer durationMinutes,
        Integer quantity,
        WaterUnit unit,
        String description,
        Integer calories,
        String activityType,
        Integer count,
        String company,
        String role,
        String status,
        String mood,
        String notes,
        Instant createdAt,
        Instant updatedAt,
        Integer applicationCount
) {
}
