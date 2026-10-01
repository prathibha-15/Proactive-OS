package com.proactiveos.events.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "study_events")
@DiscriminatorValue("STUDY")
public class StudyEvent extends LifeEvent {

    @Column(name = "subject")
    private String subject;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    protected StudyEvent() {
    }

    public StudyEvent(Long journalEntryId, Instant eventTime, EventSource source, Double confidence,
                       String subject, Integer durationMinutes) {
        super(journalEntryId, eventTime, source, confidence);
        this.subject = subject;
        this.durationMinutes = durationMinutes;
    }

    @Override
    public LifeEventType getType() {
        return LifeEventType.STUDY;
    }

    public void updateDetails(String subject, Integer durationMinutes) {
        this.subject = subject;
        this.durationMinutes = durationMinutes;
    }

    public String getSubject() {
        return subject;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }
}
