package com.proactiveos.extraction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;

import com.proactiveos.events.entity.EventSource;
import com.proactiveos.events.entity.LifeEventType;
import com.proactiveos.events.entity.JobApplicationEvent;
import com.proactiveos.events.entity.StepsEvent;
import com.proactiveos.events.entity.StudyEvent;
import com.proactiveos.events.entity.WaterEvent;
import com.proactiveos.events.entity.WaterUnit;
import com.proactiveos.events.entity.WorkoutEvent;
import com.proactiveos.events.repository.LifeEventRepository;
import com.proactiveos.journal.entity.JournalEntry;
import com.proactiveos.journal.repository.JournalEntryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class JournalExtractionDatabaseIntegrationTest {

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    @Autowired
    private LifeEventRepository lifeEventRepository;

    @Autowired
    private JournalExtractionService journalExtractionService;

    @MockBean
    private AiExtractionService aiExtractionService;

    @Test
    void persistsValidatedEventsAndLeavesOriginalJournalUnchanged() {
        JournalEntry journal = journalEntryRepository.save(JournalEntry.create(
                "I drank water, studied, worked out, walked 7000 steps, and applied to three jobs.",
                LocalDate.of(2026, 9, 28)));
        lifeEventRepository.save(new StudyEvent(journal.getId(), null, EventSource.JOURNAL, null, "Old extraction", 30));
        lifeEventRepository.save(new WaterEvent(journal.getId(), null, EventSource.MANUAL, null, 1, WaterUnit.GLASS));
        lifeEventRepository.save(new StepsEvent(journal.getId(), null, EventSource.DEVICE, null, 2500));
        when(aiExtractionService.extract(journal.getContent(), journal.getEntryDate(), ZoneId.of("UTC")))
                .thenReturn("""
                        {"events":[
                          {"type":"WATER","quantity":3,"unit":"GLASS","eventTime":null,"confidence":null},
                          {"type":"STUDY","subject":"Spring Boot","durationMinutes":120,"eventTime":null,"confidence":null},
                          {"type":"WORKOUT","activityType":"Leg+Glutes","durationMinutes":null,"eventTime":null,"confidence":null},
                          {"type":"STEPS","count":7000,"eventTime":null,"confidence":null},
                          {"type":"JOB_APPLICATION","applicationCount":3,"eventTime":null,"confidence":null}
                        ]}
                        """);

        var results = journalExtractionService.extract(journal.getId());
        var persisted = lifeEventRepository.findAllByJournalEntryIdOrderByEventTimeDesc(journal.getId());

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
                .extracting(event -> ((StepsEvent) event).getCount())
                .isEqualTo(2500);
        assertThat(persisted).filteredOn(event -> event instanceof StudyEvent)
                .extracting(event -> ((StudyEvent) event).getSubject())
                .containsExactly("Spring Boot");
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
        assertThat(journalEntryRepository.findById(journal.getId()).orElseThrow().getContent())
                .isEqualTo("I drank water, studied, worked out, walked 7000 steps, and applied to three jobs.");
    }
}
