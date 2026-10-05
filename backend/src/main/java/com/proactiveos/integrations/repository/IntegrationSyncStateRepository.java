package com.proactiveos.integrations.repository;

import java.util.Optional;

import com.proactiveos.integrations.entity.ExternalProviderId;
import com.proactiveos.integrations.entity.IntegrationSyncState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntegrationSyncStateRepository extends JpaRepository<IntegrationSyncState, Long> {

    Optional<IntegrationSyncState> findByOwner_IdAndProvider(Long ownerId, ExternalProviderId provider);
}