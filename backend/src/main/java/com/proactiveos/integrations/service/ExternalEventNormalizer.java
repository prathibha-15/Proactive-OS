package com.proactiveos.integrations.service;

import java.util.stream.Collectors;

import com.proactiveos.events.dto.EventRequest;
import com.proactiveos.events.entity.EventSource;
import com.proactiveos.events.entity.LifeEventType;
import com.proactiveos.integrations.dto.ExternalActivityRecord;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

@Component
public class ExternalEventNormalizer {

    private final Validator validator;

    public ExternalEventNormalizer(Validator validator) {
        this.validator = validator;
    }

    public EventRequest normalize(ExternalActivityRecord record) {
        if (record == null || record.externalRecordId() == null || record.externalRecordId().isBlank()
                || record.externalRecordId().length() > 200 || record.type() == null) {
            throw new InvalidExternalActivityException("External activity needs a supported type and stable record ID.");
        }

        EventRequest event = switch (record.type()) {
            case STEPS -> new EventRequest(LifeEventType.STEPS, EventSource.DEVICE, null, record.eventTime(), null,
                    null, null, null, null, null, null, null, record.count(), null, null, null, null, null, null);
            case WORKOUT -> new EventRequest(LifeEventType.WORKOUT, EventSource.DEVICE, null, record.eventTime(), null,
                    null, record.durationMinutes(), null, null, null, null, record.activityType(), null,
                    null, null, null, null, null, null);
            case SLEEP -> new EventRequest(LifeEventType.SLEEP, EventSource.DEVICE, null, record.eventTime(), null,
                    null, record.durationMinutes(), null, null, null, null, null, null,
                    null, null, null, null, record.notes(), null);
            default -> throw new InvalidExternalActivityException(
                    "External provider returned unsupported event type: " + record.type());
        };

        var violations = validator.validate(event);
        if (!violations.isEmpty()) {
            String detail = violations.stream()
                    .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                    .collect(Collectors.joining("; "));
            throw new InvalidExternalActivityException("External activity is invalid: " + detail);
        }
        boolean hasDetails = switch (record.type()) {
            case STEPS -> record.count() != null;
            case WORKOUT -> hasText(record.activityType()) || record.durationMinutes() != null;
            case SLEEP -> record.durationMinutes() != null || hasText(record.notes());
            default -> false;
        };
        if (!hasDetails) {
            throw new InvalidExternalActivityException("External activity has no supported event details.");
        }
        if ((record.type() == LifeEventType.STEPS && (record.durationMinutes() != null
                || record.activityType() != null || record.notes() != null))
                || (record.type() == LifeEventType.WORKOUT && (record.count() != null || record.notes() != null))
                || (record.type() == LifeEventType.SLEEP && (record.count() != null || record.activityType() != null))) {
            throw new InvalidExternalActivityException("External activity contains fields that do not apply to "
                    + record.type() + ".");
        }
        return event;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}