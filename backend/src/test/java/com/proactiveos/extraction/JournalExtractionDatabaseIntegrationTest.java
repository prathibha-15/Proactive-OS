package com.proactiveos.extraction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import com.proactiveos.auth.entity.User;
import com.proactiveos.auth.repository.UserRepository;
import com.proactiveos.events.entity.LifeEvent;
import com.proactiveos.events.entity.EventSource;
import com.proactiveos.events.entity.LifeEventType;
import com.proactiveos.events.entity.JobApplicationEvent;
import com.proactiveos.events.entity.StepsEvent;
import com.proactiveos.events.entity.StudyEvent;
import com.proactiveos.events.entity.WaterEvent;
import com.proactiveos.events.entity.WaterUnit;
import com.proactiveos.events.entity.WorkoutEvent;
import com.proactiveos.events.repository.LifeEventRepository;
import com.proactiveos.integrations.entity.ExternalProviderId;
import com.proactiveos.journal.entity.JournalEntry;
import com.proactiveos.journal.repository.JournalEntryRepository;
import com.proactiveos.insights.service.ProactiveInsightsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "proactiveos.ai.time-zone=Asia/Kolkata")
@Transactional
class JournalExtractionDatabaseIntegrationTest {

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    @Autowired
    private LifeEventRepository lifeEventRepository;

        @Autowired
        private UserRepository userRepository;

    @Autowired
    private JournalExtractionService journalExtractionService;

        @Autowired
        private ProactiveInsightsService insightsService;

    @MockBean
    private AiExtractionService aiExtractionService;

    @Test
    void persistsValidatedEventsAndLeavesOriginalJournalUnchanged() {
        User owner = userRepository.save(User.create("phase7-" + UUID.randomUUID() + "@example.com", "hash"));
        LocalDate entryDate = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        JournalEntry journal = journalEntryRepository.save(JournalEntry.create(
                "I drank water, studied, worked out, walked 7000 steps, and applied to three jobs.",
                entryDate, owner));
        lifeEventRepository.save(owned(new StudyEvent(journal.getId(), null, EventSource.JOURNAL, null,
                "Old extraction", 30), owner));
        lifeEventRepository.save(owned(new WaterEvent(journal.getId(), null, EventSource.MANUAL, null,
                1, WaterUnit.GLASS), owner));
        StepsEvent deviceSteps = owned(
                new StepsEvent(journal.getId(), null, EventSource.DEVICE, null, 2500), owner);
        deviceSteps.assignExternalIdentity(ExternalProviderId.MOCK, "device-steps-existing");
        lifeEventRepository.save(deviceSteps);
                                when(aiExtractionService.extract(journal.getContent(), journal.getEntryDate(), ZoneId.of("Asia/Kolkata")))
                .thenReturn("""
                        {"events":[
                                                                                                        {"type":"WATER","quantity":3,"unit":"GLASS","eventTime":"%sT09:00:00","confidence":null},
                                                                                                        {"type":"STUDY","subject":"Spring Boot","durationMinutes":120,"eventTime":"%sT10:00:00","confidence":null},
                                                                                                        {"type":"WORKOUT","activityType":"Leg+Glutes","durationMinutes":40,"eventTime":"%sT18:00:00","confidence":null},
                          {"type":"STEPS","count":7000,"eventTime":null,"confidence":null},
                                                                                                        {"type":"JOB_APPLICATION","applicationCount":3,"eventTime":"%sT13:00:00","confidence":null}
                        ]}
                                                                                                """.formatted(entryDate, entryDate, entryDate, entryDate));

        var results = journalExtractionService.extract(owner.getId(), journal.getId());
        var persisted = lifeEventRepository.findAllByJournalEntryIdAndOwner_IdOrderByEventTimeDesc(
                journal.getId(), owner.getId());

        assertThat(results).hasSize(5);
        assertThat(persisted).hasSize(7)
                .allMatch(event -> event.getJournalEntryId().equals(journal.getId()));
        assertThat(persisted).filteredOn(event -> event.getSource() == EventSource.JOURNAL)
                .extracting(event -> event.getType())
                .containsExactlyInAnyOrder(LifeEventType.WATER, LifeEventType.STUDY, LifeEventType.WORKOUT,
                        LifeEventType.STEPS, LifeEventType.JOB_APPLICATION);
        assertThat(persisted).filteredOn(event -> event.getSource() == EventSource.MANUAL)
                .singleElement()
                .extracting(event -> ((WaterEvent) event).getQuantity())
                .isEqualTo(1);
        assertThat(persisted).filteredOn(event -> event.getSource() == EventSource.DEVICE)
                .singleElement()
                                .satisfies(event -> {
                                        assertThat(((StepsEvent) event).getCount()).isEqualTo(2500);
                                        assertThat(event.getExternalProvider()).isEqualTo(ExternalProviderId.MOCK);
                                        assertThat(event.getExternalRecordId()).isEqualTo("device-steps-existing");
                                });
        assertThat(persisted).filteredOn(event -> event instanceof StudyEvent)
                .extracting(event -> ((StudyEvent) event).getSubject())
                .containsExactly("Spring Boot");
        assertThat(persisted).filteredOn(event -> event instanceof WaterEvent && event.getSource() == EventSource.JOURNAL)
                .singleElement()
                .satisfies(event -> assertThat(event.getEventTime()).isEqualTo(Instant.parse(entryDate + "T03:30:00Z")));
        assertThat(persisted).filteredOn(event -> event instanceof StudyEvent)
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.getEventTime()).isEqualTo(Instant.parse(entryDate + "T04:30:00Z"));
                    assertThat(((StudyEvent) event).getDurationMinutes()).isEqualTo(120);
                });
        assertThat(persisted).filteredOn(event -> event instanceof WorkoutEvent)
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.getEventTime()).isEqualTo(Instant.parse(entryDate + "T12:30:00Z"));
                    assertThat(((WorkoutEvent) event).getDurationMinutes()).isEqualTo(40);
                });
        assertThat(persisted).filteredOn(event -> event instanceof JobApplicationEvent)
                .singleElement()
                .satisfies(event -> assertThat(event.getEventTime()).isEqualTo(Instant.parse(entryDate + "T07:30:00Z")));
        assertThat(persisted).filteredOn(event -> event instanceof StepsEvent && event.getSource() == EventSource.JOURNAL)
                .singleElement()
                .satisfies(event -> assertThat(event.getEventTime()).isNull());
        assertThat(persisted).filteredOn(event -> event instanceof WaterEvent && event.getSource() == EventSource.JOURNAL)
                .singleElement()
                .extracting(event -> ((WaterEvent) event).getUnit())
                .isEqualTo(WaterUnit.GLASS);
        assertThat(persisted).filteredOn(event -> event instanceof WorkoutEvent)
                .singleElement()
                .extracting(event -> ((WorkoutEvent) event).getActivityType())
                .isEqualTo("Leg+Glutes");
        assertThat(persisted).filteredOn(event -> event instanceof StepsEvent && event.getSource() == EventSource.JOURNAL)
                .singleElement()
                .extracting(event -> ((StepsEvent) event).getCount())
                .isEqualTo(7000);
        assertThat(persisted).filteredOn(event -> event instanceof JobApplicationEvent)
                .singleElement()
                .extracting(event -> ((JobApplicationEvent) event).getApplicationCount())
                .isEqualTo(3);
                assertThat(journalEntryRepository.findByIdAndOwner_Id(journal.getId(), owner.getId()).orElseThrow().getContent())
                .isEqualTo("I drank water, studied, worked out, walked 7000 steps, and applied to three jobs.");

                User otherUser = userRepository.save(User.create("phase7-" + UUID.randomUUID() + "@example.com", "hash"));
                assertThat(journalEntryRepository.findByIdAndOwner_Id(journal.getId(), otherUser.getId())).isEmpty();
                assertThat(lifeEventRepository.findByIdAndOwner_Id(persisted.getFirst().getId(), otherUser.getId())).isEmpty();
                assertThat(lifeEventRepository.findAllByOwner_IdOrderByEventTimeDesc(otherUser.getId())).isEmpty();
                var insights = insightsService.getInsights(owner.getId());
                assertThat(insights.knownTimeEventCount()).isEqualTo(4);
                assertThat(insights.unknownTimeEventCount()).isEqualTo(3);
        }

        private <T extends LifeEvent> T owned(T event, User owner) {
                event.assignOwner(owner);
                return event;
    }
}
