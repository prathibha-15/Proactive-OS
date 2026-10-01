package com.proactiveos.events.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "workout_events")
@DiscriminatorValue("WORKOUT")
public class WorkoutEvent extends LifeEvent {

    @Column(name = "activity_type")
    private String activityType;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    protected WorkoutEvent() {
    }

    public WorkoutEvent(Long journalEntryId, Instant eventTime, EventSource source, Double confidence,
                         String activityType, Integer durationMinutes) {
        super(journalEntryId, eventTime, source, confidence);
        this.activityType = activityType;
        this.durationMinutes = durationMinutes;
    }

    @Override
    public LifeEventType getType() {
        return LifeEventType.WORKOUT;
    }

    public void updateDetails(String activityType, Integer durationMinutes) {
        this.activityType = activityType;
        this.durationMinutes = durationMinutes;
    }

    public String getActivityType() {
        return activityType;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }
}
