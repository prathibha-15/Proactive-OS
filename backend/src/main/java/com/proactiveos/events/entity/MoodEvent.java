package com.proactiveos.events.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "mood_events")
@DiscriminatorValue("MOOD")
public class MoodEvent extends LifeEvent {

    @Column(name = "mood")
    private String mood;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    protected MoodEvent() {
    }

    public MoodEvent(Long journalEntryId, Instant eventTime, EventSource source, Double confidence,
                      String mood, String notes) {
        super(journalEntryId, eventTime, source, confidence);
        this.mood = mood;
        this.notes = notes;
    }

    @Override
    public LifeEventType getType() {
        return LifeEventType.MOOD;
    }

    public void updateDetails(String mood, String notes) {
        this.mood = mood;
        this.notes = notes;
    }

    public String getMood() {
        return mood;
    }

    public String getNotes() {
        return notes;
    }
}
