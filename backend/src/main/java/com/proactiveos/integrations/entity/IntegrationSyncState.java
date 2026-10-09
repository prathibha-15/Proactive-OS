package com.proactiveos.integrations.entity;

import java.time.Instant;

import com.proactiveos.auth.entity.User;
import com.proactiveos.events.entity.ExternalProviderIdConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "integration_sync_states", uniqueConstraints = @UniqueConstraint(
        name = "uk_integration_sync_state_owner_provider", columnNames = {"user_id", "provider"}))
public class IntegrationSyncState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    @Convert(converter = ExternalProviderIdConverter.class)
    @Column(name = "provider", nullable = false, length = 40)
    private ExternalProviderId provider;

    @Column(name = "last_synced_at", nullable = false)
    private Instant lastSyncedAt;

    protected IntegrationSyncState() {
    }

    private IntegrationSyncState(User owner, ExternalProviderId provider) {
        this.owner = owner;
        this.provider = provider;
    }

    public static IntegrationSyncState create(User owner, ExternalProviderId provider) {
        return new IntegrationSyncState(owner, provider);
    }

    public void markSynced(Instant at) {
        lastSyncedAt = at;
    }

    public Long getId() {
        return id;
    }

    public User getOwner() {
        return owner;
    }

    public ExternalProviderId getProvider() {
        return provider;
    }

    public Instant getLastSyncedAt() {
        return lastSyncedAt;
    }
}