package com.proactiveos.events.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "sleep_events")
@DiscriminatorValue("SLEEP")
public class SleepEvent extends LifeEvent {

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    protected SleepEvent() {
    }

    public SleepEvent(Long journalEntryId, Instant eventTime, EventSource source, Double confidence,
                       Integer durationMinutes, String notes) {
        super(journalEntryId, eventTime, source, confidence);
        this.durationMinutes = durationMinutes;
        this.notes = notes;
    }

    @Override
    public LifeEventType getType() {
        return LifeEventType.SLEEP;
    }

    public void updateDetails(Integer durationMinutes, String notes) {
        this.durationMinutes = durationMinutes;
        this.notes = notes;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public String getNotes() {
        return notes;
    }
}
