package com.proactiveos.events.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "food_events")
@DiscriminatorValue("FOOD")
public class FoodEvent extends LifeEvent {

    @Column(name = "description")
    private String description;

    @Column(name = "calories")
    private Integer calories;

    protected FoodEvent() {
    }

    public FoodEvent(Long journalEntryId, Instant eventTime, EventSource source, Double confidence,
                      String description, Integer calories) {
        super(journalEntryId, eventTime, source, confidence);
        this.description = description;
        this.calories = calories;
    }

    @Override
    public LifeEventType getType() {
        return LifeEventType.FOOD;
    }

    public void updateDetails(String description, Integer calories) {
        this.description = description;
        this.calories = calories;
    }

    public String getDescription() {
        return description;
    }

    public Integer getCalories() {
        return calories;
    }
}
