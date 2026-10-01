package com.proactiveos.events.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Common fields shared by every structured event. Type-specific data lives on the
 * subclasses/subtables so this table never grows unrelated, sparsely-populated columns.
 * Not sealed/final: Hibernate must be able to generate proxy subclasses for every entity.
 */
@Entity
@Table(name = "life_events")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "event_type", discriminatorType = DiscriminatorType.STRING)
public abstract class LifeEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "journal_entry_id")
    private Long journalEntryId;

    @Column(name = "event_time")
    private Instant eventTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private EventSource source;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LifeEvent() {
    }

    protected LifeEvent(Long journalEntryId, Instant eventTime, EventSource source, Double confidence) {
        this.journalEntryId = journalEntryId;
        this.eventTime = eventTime;
        this.source = source;
        this.confidence = confidence;
    }

    @PrePersist
    void initializeTimestamps() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void touchUpdatedAt() {
        updatedAt = Instant.now();
    }

    public void updateCommon(Long journalEntryId, Instant eventTime, EventSource source, Double confidence) {
        this.journalEntryId = journalEntryId;
        if (eventTime != null) {
            this.eventTime = eventTime;
        }
        this.source = source;
        this.confidence = confidence;
    }

    public abstract LifeEventType getType();

    public Long getId() {
        return id;
    }

    public Long getJournalEntryId() {
        return journalEntryId;
    }

    public Instant getEventTime() {
        return eventTime;
    }

    public EventSource getSource() {
        return source;
    }

    public Double getConfidence() {
        return confidence;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
