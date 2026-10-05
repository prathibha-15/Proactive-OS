package com.proactiveos.events.service;

import java.time.Instant;

import com.proactiveos.events.dto.EventRequest;
import com.proactiveos.events.dto.EventResponse;
import com.proactiveos.events.entity.EventSource;
import com.proactiveos.events.entity.FoodEvent;
import com.proactiveos.events.entity.JobApplicationEvent;
import com.proactiveos.events.entity.LifeEvent;
import com.proactiveos.events.entity.MoodEvent;
import com.proactiveos.events.entity.SleepEvent;
import com.proactiveos.events.entity.StepsEvent;
import com.proactiveos.events.entity.StudyEvent;
import com.proactiveos.events.entity.WaterEvent;
import com.proactiveos.events.entity.WaterUnit;
import com.proactiveos.events.entity.WorkoutEvent;
import org.springframework.stereotype.Component;

/**
 * Translates between the flat {@link EventRequest}/{@link EventResponse} DTOs and the
 * type-specific entities.
 */
@Component
public class LifeEventMapper {

    public LifeEvent toEntity(EventRequest request) {
        Long journalEntryId = request.journalEntryId();
        Instant eventTime = request.eventTime();
        EventSource source = request.source();
        Double confidence = request.confidence();

        return switch (request.type()) {
            case SLEEP -> new SleepEvent(journalEntryId, eventTime, source, confidence,
                    request.durationMinutes(), request.notes());
            case WATER -> new WaterEvent(journalEntryId, eventTime, source, confidence,
                    request.quantity(), request.unit());
            case FOOD -> new FoodEvent(journalEntryId, eventTime, source, confidence,
                    request.description(), request.calories());
            case STUDY -> new StudyEvent(journalEntryId, eventTime, source, confidence,
                    request.subject(), request.durationMinutes());
            case WORKOUT -> new WorkoutEvent(journalEntryId, eventTime, source, confidence,
                    request.activityType(), request.durationMinutes());
            case STEPS -> new StepsEvent(journalEntryId, eventTime, source, confidence, request.count());
            case JOB_APPLICATION -> new JobApplicationEvent(journalEntryId, eventTime, source, confidence,
                    request.company(), request.role(), request.status(), request.applicationCount());
            case MOOD -> new MoodEvent(journalEntryId, eventTime, source, confidence,
                    request.mood(), request.notes());
        };
    }

    public void applyDetails(LifeEvent entity, EventRequest request) {
        switch (entity) {
            case SleepEvent e -> e.updateDetails(request.durationMinutes(), request.notes());
            case WaterEvent e -> e.updateDetails(request.quantity(), request.unit());
            case FoodEvent e -> e.updateDetails(request.description(), request.calories());
            case StudyEvent e -> e.updateDetails(request.subject(), request.durationMinutes());
            case WorkoutEvent e -> e.updateDetails(request.activityType(), request.durationMinutes());
            case StepsEvent e -> e.updateDetails(request.count());
            case JobApplicationEvent e -> e.updateDetails(request.company(), request.role(), request.status(), request.applicationCount());
            case MoodEvent e -> e.updateDetails(request.mood(), request.notes());
            default -> throw new IllegalStateException("Unhandled life event type: " + entity.getClass());
        }
    }

    public EventResponse toResponse(LifeEvent entity) {
        String subject = null;
        Integer durationMinutes = null;
        Integer quantity = null;
        WaterUnit unit = null;
        String description = null;
        Integer calories = null;
        String activityType = null;
        Integer count = null;
        String company = null;
        String role = null;
        String status = null;
        String mood = null;
        String notes = null;
        Integer applicationCount = null;

        switch (entity) {
            case SleepEvent e -> {
                durationMinutes = e.getDurationMinutes();
                notes = e.getNotes();
            }
            case WaterEvent e -> {
                quantity = e.getQuantity();
                unit = e.getUnit();
            }
            case FoodEvent e -> {
                description = e.getDescription();
                calories = e.getCalories();
            }
            case StudyEvent e -> {
                subject = e.getSubject();
                durationMinutes = e.getDurationMinutes();
            }
            case WorkoutEvent e -> {
                activityType = e.getActivityType();
                durationMinutes = e.getDurationMinutes();
            }
            case StepsEvent e -> count = e.getCount();
            case JobApplicationEvent e -> {
                company = e.getCompany();
                role = e.getRole();
                status = e.getStatus();
                applicationCount = e.getApplicationCount();
            }
            case MoodEvent e -> {
                mood = e.getMood();
                notes = e.getNotes();
            }
            default -> throw new IllegalStateException("Unhandled life event type: " + entity.getClass());
        }

        return new EventResponse(
                entity.getId(),
                entity.getType(),
                entity.getSource(),
                entity.getJournalEntryId(),
                entity.getEventTime(),
                entity.getConfidence(),
                subject,
                durationMinutes,
                quantity,
                unit,
                description,
                calories,
                activityType,
                count,
                company,
                role,
                status,
                mood,
                notes,
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                applicationCount,
                entity.getExternalProvider()
        );
    }
}
