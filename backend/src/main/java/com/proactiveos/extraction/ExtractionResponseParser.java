package com.proactiveos.extraction;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proactiveos.events.dto.EventRequest;
import com.proactiveos.events.entity.EventSource;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;

@Component
public class ExtractionResponseParser {

    private static final Set<String> EVENT_FIELDS = Set.of(
            "type", "source", "journalEntryId", "eventTime", "confidence", "subject", "durationMinutes",
            "quantity", "unit", "description", "calories", "activityType", "count", "company", "role",
            "status", "mood", "notes", "applicationCount");

    private final ObjectMapper strictMapper;
    private final SpringValidatorAdapter validator;

    public ExtractionResponseParser(ObjectMapper objectMapper, Validator validator) {
        this.strictMapper = objectMapper.copy()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        this.validator = new SpringValidatorAdapter(validator);
    }

    public List<EventRequest> parseAndValidate(String json, Long journalEntryId, LocalDate journalDate, ZoneId timeZone) {
        try {
            JsonNode root = strictMapper.readTree(json);
            if (root == null || !root.isObject() || root.size() != 1 || !root.has("events") || !root.get("events").isArray()) {
                throw new AiExtractionException("AI response must be a JSON object containing only an events array.");
            }

            List<EventRequest> requests = new ArrayList<>();
            for (JsonNode eventNode : root.get("events")) {
                if (!eventNode.isObject()) {
                    throw new AiExtractionException("Each extracted event must be a JSON object.");
                }
                eventNode.fieldNames().forEachRemaining(field -> {
                    if (!EVENT_FIELDS.contains(field)) {
                        throw new AiExtractionException("AI response contains an unsupported event field: " + field);
                    }
                });
                JsonNode eventTime = eventNode.get("eventTime");
                if (eventTime != null && !eventTime.isNull() && !eventTime.isTextual()) {
                    throw new AiExtractionException("AI event time must be a local ISO-8601 date-time, an offset date-time, or null.");
                }
                String eventTimeValue = eventTime == null || eventTime.isNull() ? null : eventTime.textValue();

                ((com.fasterxml.jackson.databind.node.ObjectNode) eventNode)
                    .remove(List.of("source", "journalEntryId", "eventTime"));
                EventRequest parsed = strictMapper.treeToValue(eventNode, EventRequest.class);
                EventRequest normalized = normalize(parsed, journalEntryId,
                    parseEventTime(eventTimeValue, journalDate, timeZone));
                validate(normalized);
                requests.add(normalized);
            }
            return List.copyOf(requests);
        } catch (JsonProcessingException exception) {
            throw new AiExtractionException("AI response was not valid extraction JSON.", exception);
        }
    }

    private Instant parseEventTime(String value, LocalDate journalDate, ZoneId timeZone) {
        if (value == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value).toInstant();
        } catch (DateTimeParseException ignored) {
            try {
                LocalDateTime localDateTime = LocalDateTime.parse(value);
                return resolveLocalDateTime(localDateTime, timeZone);
            } catch (DateTimeParseException exception) {
                try {
                    return resolveLocalDateTime(LocalTime.parse(value).atDate(journalDate), timeZone);
                } catch (DateTimeParseException timeException) {
                    throw new AiExtractionException(
                            "AI event time must be a valid ISO-8601 local time, local date-time, or offset date-time.",
                            timeException);
                }
            }
        }
    }

    private Instant resolveLocalDateTime(LocalDateTime localDateTime, ZoneId timeZone) {
        List<ZoneOffset> offsets = timeZone.getRules().getValidOffsets(localDateTime);
        if (offsets.isEmpty()) {
            throw new AiExtractionException("AI event time falls in a nonexistent local time for the configured time zone.");
        }
        if (offsets.size() > 1) {
            return null;
        }
        return localDateTime.toInstant(offsets.getFirst());
    }

    private EventRequest normalize(EventRequest request, Long journalEntryId, Instant eventTime) {
        return new EventRequest(
                request.type(), EventSource.JOURNAL, journalEntryId, eventTime, request.confidence(),
                request.subject(), request.durationMinutes(), request.quantity(), request.unit(),
                request.description(), request.calories(), request.activityType(), request.count(),
                request.company(), request.role(), request.status(), request.mood(), request.notes(),
                request.applicationCount());
    }

    private void validate(EventRequest request) {
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(request, "event");
        validator.validate(request, errors);
        if (errors.hasErrors()) {
            String details = errors.getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .collect(Collectors.joining("; "));
            throw new AiExtractionException("AI returned invalid event data: " + details);
        }
        if (request.type() == null) {
            throw new AiExtractionException("AI event type is required.");
        }

        boolean validDetails = switch (request.type()) {
            case SLEEP -> request.durationMinutes() != null || hasText(request.notes());
            case WATER -> request.quantity() != null || request.unit() != null;
            case FOOD -> hasText(request.description()) || request.calories() != null;
            case STUDY -> hasText(request.subject()) || request.durationMinutes() != null;
            case WORKOUT -> hasText(request.activityType()) || request.durationMinutes() != null;
            case STEPS -> request.count() != null;
            case JOB_APPLICATION -> request.applicationCount() != null || hasText(request.company())
                    || hasText(request.role()) || hasText(request.status());
            case MOOD -> hasText(request.mood()) || hasText(request.notes());
        };
        if (!validDetails) {
            throw new AiExtractionException("AI event " + request.type() + " has no supported event-specific details.");
        }
        validateApplicableFields(request);
    }

    private void validateApplicableFields(EventRequest request) {
        boolean valid = switch (request.type()) {
            case SLEEP -> only(request, "durationMinutes", "notes");
            case WATER -> only(request, "quantity", "unit");
            case FOOD -> only(request, "description", "calories");
            case STUDY -> only(request, "subject", "durationMinutes");
            case WORKOUT -> only(request, "activityType", "durationMinutes");
            case STEPS -> only(request, "count");
            case JOB_APPLICATION -> only(request, "company", "role", "status", "applicationCount");
            case MOOD -> only(request, "mood", "notes");
        };
        if (!valid) {
            throw new AiExtractionException("AI returned fields that do not apply to event type " + request.type() + ".");
        }
    }

    private boolean only(EventRequest request, String... allowed) {
        Set<String> allowedFields = Set.of(allowed);
        return (request.subject() == null || allowedFields.contains("subject"))
                && (request.durationMinutes() == null || allowedFields.contains("durationMinutes"))
                && (request.quantity() == null || allowedFields.contains("quantity"))
                && (request.unit() == null || allowedFields.contains("unit"))
                && (request.description() == null || allowedFields.contains("description"))
                && (request.calories() == null || allowedFields.contains("calories"))
                && (request.activityType() == null || allowedFields.contains("activityType"))
                && (request.count() == null || allowedFields.contains("count"))
                && (request.company() == null || allowedFields.contains("company"))
                && (request.role() == null || allowedFields.contains("role"))
                && (request.status() == null || allowedFields.contains("status"))
                && (request.mood() == null || allowedFields.contains("mood"))
                && (request.notes() == null || allowedFields.contains("notes"))
                && (request.applicationCount() == null || allowedFields.contains("applicationCount"));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
