package com.proactiveos.events.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.proactiveos.events.dto.EventRequest;
import com.proactiveos.events.entity.EventSource;
import com.proactiveos.events.entity.LifeEvent;
import com.proactiveos.events.entity.LifeEventType;
import com.proactiveos.events.entity.StudyEvent;
import com.proactiveos.events.entity.WaterEvent;
import com.proactiveos.events.entity.WaterUnit;
import com.proactiveos.events.repository.LifeEventRepository;
import com.proactiveos.journal.repository.JournalEntryRepository;
import com.proactiveos.journal.service.JournalNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LifeEventServiceTest {

    @Mock
    private LifeEventRepository lifeEventRepository;

    @Mock
    private JournalEntryRepository journalEntryRepository;

    @Spy
    private LifeEventMapper mapper = new LifeEventMapper();

    @InjectMocks
    private LifeEventService lifeEventService;

    private EventRequest studyRequest() {
        return new EventRequest(LifeEventType.STUDY, EventSource.MANUAL, null, Instant.parse("2026-09-24T10:00:00Z"),
            null, "Spring Boot", 120, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    private EventRequest waterRequest(Long journalEntryId) {
        return new EventRequest(LifeEventType.WATER, EventSource.MANUAL, journalEntryId,
                Instant.parse("2026-09-24T11:00:00Z"), null, null, null, 3, WaterUnit.GLASS, null, null, null, null,
                null, null, null, null, null, null);
    }

    @Test
    void createsValidStudyEvent() {
        StudyEvent entity = new StudyEvent(null, Instant.now(), EventSource.MANUAL, null, "Spring Boot", 120);
        when(lifeEventRepository.save(any(LifeEvent.class))).thenReturn(entity);

        var response = lifeEventService.create(studyRequest());

        assertThat(response.type()).isEqualTo(LifeEventType.STUDY);
        assertThat(response.subject()).isEqualTo("Spring Boot");
        assertThat(response.durationMinutes()).isEqualTo(120);
    }

    @Test
    void createsValidWaterEvent() {
        WaterEvent entity = new WaterEvent(null, Instant.now(), EventSource.MANUAL, null, 3, WaterUnit.GLASS);
        when(lifeEventRepository.save(any(LifeEvent.class))).thenReturn(entity);

        var response = lifeEventService.create(waterRequest(null));

        assertThat(response.type()).isEqualTo(LifeEventType.WATER);
        assertThat(response.quantity()).isEqualTo(3);
        assertThat(response.unit()).isEqualTo(WaterUnit.GLASS);
    }

    @Test
    void rejectsEventReferencingMissingJournal() {
        when(journalEntryRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> lifeEventService.create(waterRequest(99L)))
                .isInstanceOf(JournalNotFoundException.class);
    }

    @Test
    void retrievesEventById() {
        StudyEvent entity = new StudyEvent(null, Instant.now(), EventSource.MANUAL, null, "Spring Boot", 120);
        when(lifeEventRepository.findById(1L)).thenReturn(Optional.of(entity));

        assertThat(lifeEventService.findById(1L).subject()).isEqualTo("Spring Boot");
    }

    @Test
    void rejectsMissingEvent() {
        when(lifeEventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lifeEventService.findById(99L))
                .isInstanceOf(LifeEventNotFoundException.class);
    }

    @Test
    void updatesEventDetails() {
        StudyEvent entity = new StudyEvent(null, Instant.now(), EventSource.MANUAL, null, "Before", 60);
        when(lifeEventRepository.findById(1L)).thenReturn(Optional.of(entity));

        var request = new EventRequest(LifeEventType.STUDY, EventSource.MANUAL, null, null, null,
            "After", 90, null, null, null, null, null, null, null, null, null, null, null, null);
        var response = lifeEventService.update(1L, request);

        assertThat(response.subject()).isEqualTo("After");
        assertThat(response.durationMinutes()).isEqualTo(90);
    }

    @Test
    void rejectsChangingEventTypeOnUpdate() {
        StudyEvent entity = new StudyEvent(null, Instant.now(), EventSource.MANUAL, null, "Subject", 60);
        when(lifeEventRepository.findById(1L)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> lifeEventService.update(1L, waterRequest(null)))
                .isInstanceOf(EventTypeMismatchException.class);
    }

    @Test
    void deletesEvent() {
        StudyEvent entity = new StudyEvent(null, Instant.now(), EventSource.MANUAL, null, "Subject", 60);
        when(lifeEventRepository.findById(1L)).thenReturn(Optional.of(entity));

        lifeEventService.delete(1L);

        verify(lifeEventRepository).delete(entity);
    }

    @Test
    void preservesSourceProvenance() {
        WaterEvent entity = new WaterEvent(null, Instant.now(), EventSource.MANUAL, null, 3, WaterUnit.GLASS);
        when(lifeEventRepository.save(any(LifeEvent.class))).thenReturn(entity);

        var response = lifeEventService.create(waterRequest(null));

        assertThat(response.source()).isEqualTo(EventSource.MANUAL);
    }

    @Test
    void leavesUnknownOptionalFieldsNull() {
        var request = new EventRequest(LifeEventType.SLEEP, EventSource.MANUAL, null, null, null,
            null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        when(lifeEventRepository.save(any(LifeEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = lifeEventService.create(request);

        assertThat(response.durationMinutes()).isNull();
        assertThat(response.notes()).isNull();
    }

    @Test
    void returnsMultipleEventsBelongingToOneJournal() {
        when(journalEntryRepository.existsById(5L)).thenReturn(true);
        StudyEvent studyEvent = new StudyEvent(5L, Instant.now(), EventSource.MANUAL, null, "Spring Boot", 120);
        WaterEvent waterEvent = new WaterEvent(5L, Instant.now(), EventSource.MANUAL, null, 3, WaterUnit.GLASS);
        when(lifeEventRepository.findAllByJournalEntryIdOrderByEventTimeDesc(5L))
                .thenReturn(List.of(waterEvent, studyEvent));

        var responses = lifeEventService.findByJournal(5L);

        assertThat(responses).hasSize(2)
                .extracting("type")
                .containsExactlyInAnyOrder(LifeEventType.STUDY, LifeEventType.WATER);
    }
}
