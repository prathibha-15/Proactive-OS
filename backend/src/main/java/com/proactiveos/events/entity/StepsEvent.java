package com.proactiveos.events.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "steps_events")
@DiscriminatorValue("STEPS")
public class StepsEvent extends LifeEvent {

    @Column(name = "count")
    private Integer count;

    protected StepsEvent() {
    }

    public StepsEvent(Long journalEntryId, Instant eventTime, EventSource source, Double confidence, Integer count) {
        super(journalEntryId, eventTime, source, confidence);
        this.count = count;
    }

    @Override
    public LifeEventType getType() {
        return LifeEventType.STEPS;
    }

    public void updateDetails(Integer count) {
        this.count = count;
    }

    public Integer getCount() {
        return count;
    }
}
