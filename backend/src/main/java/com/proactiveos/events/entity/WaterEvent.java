package com.proactiveos.events.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "water_events")
@DiscriminatorValue("WATER")
public class WaterEvent extends LifeEvent {

    @Column(name = "quantity")
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit", length = 10)
    private WaterUnit unit;

    protected WaterEvent() {
    }

    public WaterEvent(Long journalEntryId, Instant eventTime, EventSource source, Double confidence,
                       Integer quantity, WaterUnit unit) {
        super(journalEntryId, eventTime, source, confidence);
        this.quantity = quantity;
        this.unit = unit;
    }

    @Override
    public LifeEventType getType() {
        return LifeEventType.WATER;
    }

    public void updateDetails(Integer quantity, WaterUnit unit) {
        this.quantity = quantity;
        this.unit = unit;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public WaterUnit getUnit() {
        return unit;
    }
}
