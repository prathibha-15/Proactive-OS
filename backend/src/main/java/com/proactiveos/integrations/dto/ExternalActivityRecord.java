package com.proactiveos.integrations.dto;

import java.time.Instant;

import com.proactiveos.events.entity.LifeEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ExternalActivityRecord(
        @NotBlank @Size(max = 200) String externalRecordId,
        @NotNull LifeEventType type,
        Instant eventTime,
        @Positive Integer durationMinutes,
        @PositiveOrZero Integer count,
        @Size(max = 100) String activityType,
        @Size(max = 1000) String notes
) {
}