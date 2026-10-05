package com.proactiveos.integrations.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.proactiveos.auth.entity.User;
import com.proactiveos.auth.repository.UserRepository;
import com.proactiveos.events.entity.EventSource;
import com.proactiveos.events.entity.LifeEvent;
import com.proactiveos.events.entity.LifeEventType;
import com.proactiveos.events.entity.SleepEvent;
import com.proactiveos.events.entity.StepsEvent;
import com.proactiveos.events.entity.WorkoutEvent;
import com.proactiveos.events.repository.LifeEventRepository;
import com.proactiveos.events.service.LifeEventService;
import com.proactiveos.insights.service.ProactiveInsightsService;
import com.proactiveos.integrations.dto.ExternalActivityRecord;
import com.proactiveos.integrations.entity.ExternalProviderId;
import com.proactiveos.integrations.provider.ExternalProviderRegistry;
import com.proactiveos.integrations.repository.IntegrationSyncStateRepository;
import com.proactiveos.journal.entity.JournalEntry;
import com.proactiveos.journal.repository.JournalEntryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ExternalDataSyncDatabaseIntegrationTest {

    private static final Instant JOURNAL_ACTIVITY_TIME = Instant.parse("2026-10-03T18:00:00Z");

    @Autowired
    private ExternalDataSyncService syncService;

    @Autowired
    private ExternalDataPersistenceService persistenceService;

    @Autowired
    private ExternalProviderRegistry providerRegistry;

    @Autowired
    private LifeEventRepository lifeEventRepository;

        @Autowired
        private LifeEventService lifeEventService;

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IntegrationSyncStateRepository syncStateRepository;

    @Autowired
    private ProactiveInsightsService insightsService;

    @Test
    void syncIsIdempotentUserScopedAndPreservesOtherSourcesInInsights() {
        User owner = saveUser();
        JournalEntry journal = journalEntryRepository.save(JournalEntry.create(
                "I worked out with a leg workout.", LocalDate.of(2026, 10, 3), owner));
        WorkoutEvent journalWorkout = new WorkoutEvent(journal.getId(), JOURNAL_ACTIVITY_TIME,
                EventSource.JOURNAL, null, "Leg workout", 40);
        journalWorkout.assignOwner(owner);
        lifeEventRepository.save(journalWorkout);
        StepsEvent manualSteps = new StepsEvent(null, JOURNAL_ACTIVITY_TIME, EventSource.MANUAL,
                null, 8200);
        manualSteps.assignOwner(owner);
        lifeEventRepository.save(manualSteps);

        var first = syncService.sync(owner.getId(), ExternalProviderId.MOCK);
        assertThat(first.eventsFetched()).isEqualTo(5);
        assertThat(first.eventsCreated()).isEqualTo(5);
        assertThat(first.eventsUpdated()).isZero();
        assertThat(first.eventsSkipped()).isZero();
        assertThat(first.lastSyncedAt()).isNotNull();

        var firstOwnerEvents = lifeEventRepository.findAllByOwner_IdOrderByEventTimeDesc(owner.getId());
        assertThat(firstOwnerEvents).hasSize(7);
        assertThat(firstOwnerEvents).filteredOn(event -> event.getSource() == EventSource.DEVICE)
                .hasSize(5)
                .allSatisfy(event -> {
                    assertThat(event.getOwner().getId()).isEqualTo(owner.getId());
                    assertThat(event.getJournalEntryId()).isNull();
                    assertThat(event.getExternalProvider()).isEqualTo(ExternalProviderId.MOCK);
                    assertThat(event.getExternalRecordId()).isNotBlank();
                });
                assertThat(firstOwnerEvents).filteredOn(event -> "steps-2026-10-03".equals(event.getExternalRecordId()))
                                .singleElement()
                                .satisfies(event -> {
                                        assertThat(event.getEventTime()).isEqualTo(Instant.parse("2026-10-03T23:59:59Z"));
                                        assertThat(((StepsEvent) event).getCount()).isEqualTo(8247);
                                });
                assertThat(firstOwnerEvents).filteredOn(event -> event instanceof SleepEvent)
                                .singleElement()
                                .satisfies(event -> {
                                        assertThat(event.getEventTime()).isEqualTo(Instant.parse("2026-10-03T00:30:00Z"));
                                        assertThat(((SleepEvent) event).getDurationMinutes()).isEqualTo(480);
                                });
                assertThat(firstOwnerEvents).filteredOn(event -> event instanceof WorkoutEvent
                                && event.getSource() == EventSource.DEVICE)
                                .allSatisfy(event -> {
                                        assertThat(event.getEventTime()).isNotNull();
                                        assertThat(((WorkoutEvent) event).getDurationMinutes()).isEqualTo(40);
                                });
        assertThat(firstOwnerEvents).filteredOn(event -> event instanceof StepsEvent)
                .extracting(event -> ((StepsEvent) event).getCount())
                .containsExactlyInAnyOrder(8200, 8247);
        assertThat(firstOwnerEvents).filteredOn(event -> event.getSource() == EventSource.JOURNAL)
                .singleElement()
                .extracting(LifeEvent::getType)
                .isEqualTo(LifeEventType.WORKOUT);
        assertThat(firstOwnerEvents).filteredOn(event -> event.getSource() == EventSource.MANUAL)
                .singleElement()
                .extracting(LifeEvent::getType)
                .isEqualTo(LifeEventType.STEPS);

        var repeated = syncService.sync(owner.getId(), ExternalProviderId.MOCK);
        assertThat(repeated.eventsFetched()).isEqualTo(5);
        assertThat(repeated.eventsCreated()).isZero();
        assertThat(repeated.eventsSkipped()).isEqualTo(5);
        assertThat(lifeEventRepository.findAllByOwner_IdOrderByEventTimeDesc(owner.getId())).hasSize(7);

        var newRecord = new ExternalActivityRecord("workout-new-record", LifeEventType.WORKOUT,
                Instant.parse("2026-10-03T20:00:00Z"), 45, null, "Leg workout", null);
        var newRecordResult = persistenceService.persist(owner.getId(), providerRegistry.get(ExternalProviderId.MOCK),
                java.util.List.of(newRecord));
        assertThat(newRecordResult.eventsCreated()).isEqualTo(1);
        assertThat(lifeEventRepository.findAllByOwner_IdOrderByEventTimeDesc(owner.getId())).hasSize(8);

        User otherUser = saveUser();
        assertThat(syncService.getProviders(otherUser.getId())).singleElement()
                .satisfies(status -> assertThat(status.lastSyncedAt()).isNull());
        assertThat(lifeEventRepository.findAllByOwner_IdOrderByEventTimeDesc(otherUser.getId())).isEmpty();
        var otherUserSync = syncService.sync(otherUser.getId(), ExternalProviderId.MOCK);
        assertThat(otherUserSync.eventsCreated()).isEqualTo(5);
        assertThat(lifeEventRepository.findAllByOwner_IdOrderByEventTimeDesc(otherUser.getId())).hasSize(5);
        LifeEvent ownerEvent = lifeEventRepository.findByOwner_IdAndExternalProviderAndExternalRecordId(
                owner.getId(), ExternalProviderId.MOCK, "steps-2026-10-03").orElseThrow();
        assertThat(lifeEventRepository.findByIdAndOwner_Id(ownerEvent.getId(), otherUser.getId())).isEmpty();
        var eventResponse = lifeEventService.findById(owner.getId(), ownerEvent.getId());
        assertThat(eventResponse.source()).isEqualTo(EventSource.DEVICE);
        assertThat(eventResponse.externalProvider()).isEqualTo(ExternalProviderId.MOCK);
        assertThat(syncStateRepository.findByOwner_IdAndProvider(owner.getId(), ExternalProviderId.MOCK))
                .get().extracting(state -> state.getLastSyncedAt()).isNotNull();

        var insights = insightsService.getInsights(owner.getId());
        assertThat(insights.knownTimeEventCount()).isEqualTo(8);
        assertThat(insights.unknownTimeEventCount()).isZero();
        assertThat(insights.eventCounts()).contains(
                new com.proactiveos.insights.dto.EventTypeCount(LifeEventType.STEPS, 2),
                new com.proactiveos.insights.dto.EventTypeCount(LifeEventType.WORKOUT, 5));
        assertThat(insights.repeatedActivities()).anySatisfy(observation -> {
            assertThat(observation.type()).isEqualTo(LifeEventType.WORKOUT);
            assertThat(observation.label()).isEqualTo("Leg workout");
            assertThat(observation.distinctDays()).isEqualTo(3);
            assertThat(observation.eventCount()).isEqualTo(5);
        });
        assertThat(insights.recommendations()).anySatisfy(recommendation ->
                assertThat(recommendation.id()).isEqualTo("workout:leg workout"));
    }

    private User saveUser() {
        return userRepository.save(User.create("integration-" + UUID.randomUUID() + "@example.test", "hash"));
    }
}