package com.proactiveos.integrations.service;

import java.time.Instant;
import java.util.List;

import com.proactiveos.integrations.dto.ExternalActivityRecord;
import com.proactiveos.integrations.dto.IntegrationProviderStatus;
import com.proactiveos.integrations.dto.IntegrationSyncResponse;
import com.proactiveos.integrations.entity.ExternalProviderId;
import com.proactiveos.integrations.provider.ExternalDataProvider;
import com.proactiveos.integrations.provider.ExternalProviderRegistry;
import com.proactiveos.integrations.repository.IntegrationSyncStateRepository;
import org.springframework.stereotype.Service;

@Service
public class ExternalDataSyncService {

    private final ExternalProviderRegistry providerRegistry;
    private final ExternalDataPersistenceService persistenceService;
    private final IntegrationSyncStateRepository syncStateRepository;

    public ExternalDataSyncService(ExternalProviderRegistry providerRegistry,
                                   ExternalDataPersistenceService persistenceService,
                                   IntegrationSyncStateRepository syncStateRepository) {
        this.providerRegistry = providerRegistry;
        this.persistenceService = persistenceService;
        this.syncStateRepository = syncStateRepository;
    }

    public List<IntegrationProviderStatus> getProviders(Long ownerId) {
        return providerRegistry.all().stream()
                .map(provider -> new IntegrationProviderStatus(
                        provider.providerId(),
                        provider.displayName(),
                        provider.developmentOnly(),
                        provider.supportedEventTypes().stream().sorted().toList(),
                        syncStateRepository.findByOwner_IdAndProvider(ownerId, provider.providerId())
                                .map(state -> state.getLastSyncedAt())
                                .orElse(null)))
                .toList();
    }

    public IntegrationSyncResponse sync(Long ownerId, ExternalProviderId providerId) {
        ExternalDataProvider provider = providerRegistry.get(providerId);
        List<ExternalActivityRecord> records = provider.fetchRecords(ownerId);
        if (records == null) {
            throw new InvalidExternalActivityException("External provider returned no record collection.");
        }
        return persistenceService.persist(ownerId, provider, records);
    }
}