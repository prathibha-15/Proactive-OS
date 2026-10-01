package com.proactiveos.events.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "job_application_events")
@DiscriminatorValue("JOB_APPLICATION")
public class JobApplicationEvent extends LifeEvent {

    @Column(name = "company")
    private String company;

    @Column(name = "role")
    private String role;

    @Column(name = "status")
    private String status;

    @Column(name = "application_count")
    private Integer applicationCount;

    protected JobApplicationEvent() {
    }

    public JobApplicationEvent(Long journalEntryId, Instant eventTime, EventSource source, Double confidence,
                                String company, String role, String status, Integer applicationCount) {
        super(journalEntryId, eventTime, source, confidence);
        this.company = company;
        this.role = role;
        this.status = status;
        this.applicationCount = applicationCount;
    }

    @Override
    public LifeEventType getType() {
        return LifeEventType.JOB_APPLICATION;
    }

    public void updateDetails(String company, String role, String status, Integer applicationCount) {
        this.company = company;
        this.role = role;
        this.status = status;
        this.applicationCount = applicationCount;
    }

    public String getCompany() {
        return company;
    }

    public String getRole() {
        return role;
    }

    public String getStatus() {
        return status;
    }

    public Integer getApplicationCount() {
        return applicationCount;
    }
}
