package com.proactiveos.events.dto;

import java.time.Instant;

import com.proactiveos.events.entity.EventSource;
import com.proactiveos.events.entity.LifeEventType;
import com.proactiveos.events.entity.WaterUnit;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Flat request covering every event type; only the fields relevant to {@link #type()} are read.
 * Fields left out entirely represent unknown information and are never defaulted.
 */
public record EventRequest(
        @NotNull(message = "Type is required.") LifeEventType type,
        @NotNull(message = "Source is required.") EventSource source,
        Long journalEntryId,
        Instant eventTime,
        @DecimalMin(value = "0.0", message = "Confidence must be between 0 and 1.")
        @DecimalMax(value = "1.0", message = "Confidence must be between 0 and 1.") Double confidence,
        @Size(max = 200, message = "Subject must be at most 200 characters.") String subject,
        @Positive(message = "Duration must be positive.") Integer durationMinutes,
        @Positive(message = "Quantity must be positive.") Integer quantity,
        WaterUnit unit,
        @Size(max = 500, message = "Description must be at most 500 characters.") String description,
        @PositiveOrZero(message = "Calories must not be negative.") Integer calories,
        @Size(max = 100, message = "Activity type must be at most 100 characters.") String activityType,
        @PositiveOrZero(message = "Step count must not be negative.") Integer count,
        @Size(max = 200, message = "Company must be at most 200 characters.") String company,
        @Size(max = 200, message = "Role must be at most 200 characters.") String role,
        @Size(max = 100, message = "Status must be at most 100 characters.") String status,
        @Size(max = 100, message = "Mood must be at most 100 characters.") String mood,
        @Size(max = 1000, message = "Notes must be at most 1000 characters.") String notes,
        @Positive(message = "Application count must be positive.") Integer applicationCount
) {
}
