package com.proactiveos.integrations.service;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.proactiveos.auth.entity.User;
import com.proactiveos.auth.repository.UserRepository;
import com.proactiveos.events.entity.LifeEvent;
import com.proactiveos.events.repository.LifeEventRepository;
import com.proactiveos.events.service.LifeEventMapper;
import com.proactiveos.integrations.dto.ExternalActivityRecord;
import com.proactiveos.integrations.dto.IntegrationSyncResponse;
import com.proactiveos.integrations.entity.ExternalProviderId;
import com.proactiveos.integrations.entity.IntegrationSyncState;
import com.proactiveos.integrations.provider.ExternalDataProvider;
import com.proactiveos.integrations.repository.IntegrationSyncStateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExternalDataPersistenceService {

    private final UserRepository userRepository;
    private final LifeEventRepository lifeEventRepository;
    private final IntegrationSyncStateRepository syncStateRepository;
    private final ExternalEventNormalizer normalizer;
    private final LifeEventMapper mapper;

    public ExternalDataPersistenceService(UserRepository userRepository,
                                          LifeEventRepository lifeEventRepository,
                                          IntegrationSyncStateRepository syncStateRepository,
                                          ExternalEventNormalizer normalizer,
                                          LifeEventMapper mapper) {
        this.userRepository = userRepository;
        this.lifeEventRepository = lifeEventRepository;
        this.syncStateRepository = syncStateRepository;
        this.normalizer = normalizer;
        this.mapper = mapper;
    }

    @Transactional
    public IntegrationSyncResponse persist(Long ownerId, ExternalDataProvider provider,
                                           List<ExternalActivityRecord> records) {
        User owner = userRepository.findByIdForUpdate(ownerId)
                .orElseThrow(() -> new InvalidExternalActivityException("Authenticated user no longer exists."));
        int created = 0;
        int skipped = 0;
        Set<String> seenInBatch = new HashSet<>();
        for (ExternalActivityRecord record : records) {
            if (!provider.supportedEventTypes().contains(record.type())) {
                throw new InvalidExternalActivityException("Provider returned unsupported event type: " + record.type());
            }
            var request = normalizer.normalize(record);
            if (!seenInBatch.add(record.externalRecordId())
                    || lifeEventRepository.findByOwner_IdAndExternalProviderAndExternalRecordId(
                            ownerId, provider.providerId(), record.externalRecordId()).isPresent()) {
                skipped++;
                continue;
            }
            LifeEvent event = mapper.toEntity(request);
            event.assignOwner(owner);
            event.assignExternalIdentity(provider.providerId(), record.externalRecordId());
            lifeEventRepository.save(event);
            created++;
        }

        Instant lastSyncedAt = Instant.now();
        IntegrationSyncState state = syncStateRepository.findByOwner_IdAndProvider(ownerId, provider.providerId())
                .orElseGet(() -> IntegrationSyncState.create(owner, provider.providerId()));
        state.markSynced(lastSyncedAt);
        syncStateRepository.save(state);
        return new IntegrationSyncResponse(provider.providerId(), records.size(), created, 0, skipped, lastSyncedAt);
    }
}