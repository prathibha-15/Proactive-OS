package com.proactiveos.events.entity;

import java.time.Instant;

import com.proactiveos.auth.entity.User;
import com.proactiveos.integrations.entity.ExternalProviderId;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Common fields shared by every structured event. Type-specific data lives on the
 * subclasses/subtables so this table never grows unrelated, sparsely-populated columns.
 * Not sealed/final: Hibernate must be able to generate proxy subclasses for every entity.
 */
@Entity
@Table(name = "life_events", uniqueConstraints = @UniqueConstraint(
    name = "uk_life_event_external_record",
    columnNames = {"user_id", "external_provider", "external_record_id"}))
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User owner;

    @Enumerated(EnumType.STRING)
    @Column(name = "external_provider", length = 40)
    private ExternalProviderId externalProvider;

    @Column(name = "external_record_id", length = 200)
    private String externalRecordId;

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

    public void assignOwner(User owner) {
        this.owner = owner;
    }

    public void assignExternalIdentity(ExternalProviderId provider, String externalRecordId) {
        this.externalProvider = provider;
        this.externalRecordId = externalRecordId;
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

    public User getOwner() {
        return owner;
    }

    public ExternalProviderId getExternalProvider() {
        return externalProvider;
    }

    public String getExternalRecordId() {
        return externalRecordId;
    }
}
