package com.proactiveos.extraction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.read.ListAppender;
import com.proactiveos.events.dto.EventRequest;
import com.proactiveos.events.entity.EventSource;
import com.proactiveos.events.entity.LifeEvent;
import com.proactiveos.events.entity.LifeEventType;
import com.proactiveos.events.entity.StudyEvent;
import com.proactiveos.events.entity.WaterEvent;
import com.proactiveos.events.entity.WaterUnit;
import com.proactiveos.events.repository.LifeEventRepository;
import com.proactiveos.events.service.LifeEventMapper;
import com.proactiveos.events.service.LifeEventService;
import com.proactiveos.journal.entity.JournalEntry;
import com.proactiveos.journal.repository.JournalEntryRepository;
import com.proactiveos.journal.service.JournalNotFoundException;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class JournalExtractionServiceTest {

    private static final Long JOURNAL_ID = 42L;
    private static final LocalDate ENTRY_DATE = LocalDate.of(2026, 9, 28);

    @Mock
    private JournalEntryRepository journalEntryRepository;

    @Mock
    private LifeEventRepository lifeEventRepository;

    @Mock
    private AiExtractionService aiExtractionService;

        private ExtractionResponseParser parser;
    private JournalExtractionService extractionService;

    @BeforeEach
    void setUp() {
        var validator = Validation.buildDefaultValidatorFactory().getValidator();
        parser = new ExtractionResponseParser(new ObjectMapper().registerModule(new JavaTimeModule()), validator);
        var eventService = new LifeEventService(lifeEventRepository, journalEntryRepository, new LifeEventMapper());
        extractionService = new JournalExtractionService(
                journalEntryRepository,
                aiExtractionService,
                parser,
                eventService,
                new AiProviderProperties("", "", "", "UTC"));
        Mockito.lenient().when(journalEntryRepository.findById(JOURNAL_ID)).thenReturn(Optional.of(
                JournalEntry.create("I studied Spring Boot and drank water.", ENTRY_DATE)));
        Mockito.lenient().when(journalEntryRepository.existsById(JOURNAL_ID)).thenReturn(true);
        Mockito.lenient().when(lifeEventRepository.findAllByJournalEntryIdAndSource(JOURNAL_ID, EventSource.JOURNAL))
                .thenReturn(List.of());
        Mockito.lenient().when(lifeEventRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void extractsMultipleTypesAndAssociatesThemWithTheJournal() {
        when(aiExtractionService.extract("I studied Spring Boot and drank water.", ENTRY_DATE, ZoneId.of("UTC")))
                .thenReturn("""
                        {"events":[
                          {"type":"STUDY","subject":"Spring Boot","durationMinutes":120,"eventTime":"2026-09-28T10:00:00Z"},
                          {"type":"WATER","quantity":2,"unit":"GLASS"},
                          {"type":"JOB_APPLICATION","applicationCount":3}
                        ]}
                        """);

        var results = extractionService.extract(JOURNAL_ID);

        assertThat(results).extracting("type")
                .containsExactly(LifeEventType.STUDY, LifeEventType.WATER, LifeEventType.JOB_APPLICATION);
        assertThat(results).allMatch(event -> event.source() == EventSource.JOURNAL
                && event.journalEntryId().equals(JOURNAL_ID));
        ArgumentCaptor<List<LifeEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(lifeEventRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).extracting(LifeEvent::getJournalEntryId).containsOnly(JOURNAL_ID);
        assertThat(captor.getValue()).extracting(LifeEvent::getSource).containsOnly(EventSource.JOURNAL);
        assertThat(((StudyEvent) captor.getValue().getFirst()).getSubject()).isEqualTo("Spring Boot");
        assertThat(((WaterEvent) captor.getValue().get(1)).getUnit()).isEqualTo(WaterUnit.GLASS);
    }

        @Test
        void extractsLegAndGlutesWorkoutWithoutInventingAnExactDurationFromMoreThanThirtyMinutes() {
                String journalText = "Leg+Glutes workout(>30 mins)";
                Mockito.when(journalEntryRepository.findById(JOURNAL_ID)).thenReturn(Optional.of(
                                JournalEntry.create(journalText, ENTRY_DATE)));
                when(aiExtractionService.extract(journalText, ENTRY_DATE, ZoneId.of("UTC")))
                                .thenReturn("""
                                        {"events":[{"type":"WORKOUT","eventTime":null,"confidence":null,
                                        "subject":null,"durationMinutes":null,"quantity":null,"unit":null,
                                        "description":null,"calories":null,"activityType":"Leg+Glutes",
                                        "count":null,"company":null,"role":null,"status":null,"mood":null,
                                        "notes":null,"applicationCount":null}]}
                                        """);

                var results = extractionService.extract(JOURNAL_ID);

                assertThat(results).hasSize(1);
                assertThat(results.getFirst().type()).isEqualTo(LifeEventType.WORKOUT);
                assertThat(results.getFirst().activityType()).isEqualTo("Leg+Glutes");
                assertThat(results.getFirst().durationMinutes()).isNull();
                assertThat(results.getFirst().source()).isEqualTo(EventSource.JOURNAL);
        }

        @Test
        void rejectsNonNullCrossTypeFieldsInSchemaCompleteWorkoutObject() {
                String journalText = "Leg+Glutes workout(>30 mins)";
                Mockito.when(journalEntryRepository.findById(JOURNAL_ID)).thenReturn(Optional.of(
                                JournalEntry.create(journalText, ENTRY_DATE)));
                when(aiExtractionService.extract(journalText, ENTRY_DATE, ZoneId.of("UTC")))
                                .thenReturn("""
                                        {"events":[{"type":"WORKOUT","eventTime":null,"confidence":null,
                                        "subject":null,"durationMinutes":null,"quantity":2,"unit":null,
                                        "description":null,"calories":null,"activityType":"Leg+Glutes",
                                        "count":null,"company":null,"role":null,"status":null,"mood":null,
                                        "notes":null,"applicationCount":null}]}
                                        """);

                assertThatThrownBy(() -> extractionService.extract(JOURNAL_ID))
                                .isInstanceOf(AiExtractionException.class)
                                .hasMessageContaining("fields that do not apply to event type WORKOUT");
                verify(lifeEventRepository, never()).saveAll(anyList());
                verify(lifeEventRepository, never()).deleteAll(anyList());
        }

        @Test
        void acceptsTheExpectedTopLevelEventsArrayShape() {
                List<EventRequest> requests = parser.parseAndValidate("""
                                {"events":[{"type":"STUDY","subject":"Spring Boot","durationMinutes":120}]}
                                """, JOURNAL_ID);

                assertThat(requests).hasSize(1);
                assertThat(requests.getFirst().type()).isEqualTo(LifeEventType.STUDY);
                assertThat(requests.getFirst().source()).isEqualTo(EventSource.JOURNAL);
                assertThat(requests.getFirst().journalEntryId()).isEqualTo(JOURNAL_ID);
                assertThat(requests.getFirst().subject()).isEqualTo("Spring Boot");
                assertThat(requests.getFirst().durationMinutes()).isEqualTo(120);
        }

        @Test
        void stillRejectsAdditionalTopLevelJsonProperties() {
                assertThatThrownBy(() -> parser.parseAndValidate(
                                "{\"events\":[],\"explanation\":\"extra text\"}", JOURNAL_ID))
                                .isInstanceOf(AiExtractionException.class)
                                .hasMessageContaining("only an events array");
        }

        @Test
        void optInSchemaInvalidJsonDiagnosticRedactsApiKeyAndJournalText() {
                String apiKey = "test-provider-key";
                String journalText = "I studied private subject matter.";
                Mockito.when(journalEntryRepository.findById(JOURNAL_ID)).thenReturn(Optional.of(
                                JournalEntry.create(journalText, ENTRY_DATE)));
                extractionService = new JournalExtractionService(
                                journalEntryRepository,
                                aiExtractionService,
                                parser,
                                new LifeEventService(lifeEventRepository, journalEntryRepository, new LifeEventMapper()),
                                new AiProviderProperties("https://provider.test/openai", "test-model", apiKey, "UTC"));
                ReflectionTestUtils.setField(extractionService, "logRawResponse", true);
                String schemaInvalidResponse = "{\"result\":\"not an events array\",\"token\":\"" + apiKey
                                + "\",\"journal\":\"" + journalText + "\"}";
                when(aiExtractionService.extract(journalText, ENTRY_DATE, ZoneId.of("UTC"))).thenReturn(schemaInvalidResponse);

                Logger logger = (Logger) LoggerFactory.getLogger(JournalExtractionService.class);
                ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> appender = new ListAppender<>();
                appender.start();
                logger.addAppender(appender);
                try {
                        assertThatThrownBy(() -> extractionService.extract(JOURNAL_ID))
                                        .isInstanceOf(AiExtractionException.class)
                                        .hasMessageContaining("only an events array");

                        String logs = appender.list.stream().map(event -> event.getFormattedMessage()).reduce("", String::concat);
                        assertThat(logs).contains("AI RAW EXTRACTION RESPONSE:")
                                        .contains("not an events array")
                                        .contains("[REDACTED]")
                                        .contains("[JOURNAL_TEXT_REDACTED]")
                                        .doesNotContain(apiKey)
                                        .doesNotContain(journalText);
                } finally {
                        logger.detachAppender(appender);
                }
        }

    @Test
    void acceptsAnEmptyExtractionAndReplacesPriorJournalEventsWithNone() {
        when(aiExtractionService.extract("I studied Spring Boot and drank water.", ENTRY_DATE, ZoneId.of("UTC")))
                .thenReturn("{\"events\":[]}");

        assertThat(extractionService.extract(JOURNAL_ID)).isEmpty();
        verify(lifeEventRepository).deleteAll(List.of());
        verify(lifeEventRepository).saveAll(List.of());
    }

    @Test
    void rejectsMalformedJsonBeforePersistence() {
        when(aiExtractionService.extract("I studied Spring Boot and drank water.", ENTRY_DATE, ZoneId.of("UTC")))
                .thenReturn("not json");

        assertThatThrownBy(() -> extractionService.extract(JOURNAL_ID))
                .isInstanceOf(AiExtractionException.class)
                .hasMessageContaining("valid extraction JSON");
        verify(lifeEventRepository, never()).deleteAll(anyList());
        verify(lifeEventRepository, never()).saveAll(anyList());
    }

    @Test
    void rejectsUnknownEventTypesBeforePersistence() {
        when(aiExtractionService.extract("I studied Spring Boot and drank water.", ENTRY_DATE, ZoneId.of("UTC")))
                .thenReturn("{\"events\":[{\"type\":\"TELEPORT\"}]}");

        assertThatThrownBy(() -> extractionService.extract(JOURNAL_ID))
                .isInstanceOf(AiExtractionException.class);
        verify(lifeEventRepository, never()).deleteAll(anyList());
    }

    @Test
    void rejectsInvalidEventSpecificDataBeforePersistence() {
        when(aiExtractionService.extract("I studied Spring Boot and drank water.", ENTRY_DATE, ZoneId.of("UTC")))
                .thenReturn("{\"events\":[{\"type\":\"WATER\",\"quantity\":-2,\"unit\":\"GLASS\"}]}");

        assertThatThrownBy(() -> extractionService.extract(JOURNAL_ID))
                .isInstanceOf(AiExtractionException.class)
                .hasMessageContaining("invalid event data");
        verify(lifeEventRepository, never()).deleteAll(anyList());
    }

        @Test
        void rejectsNonIsoEventTimeBeforePersistence() {
                when(aiExtractionService.extract("I studied Spring Boot and drank water.", ENTRY_DATE, ZoneId.of("UTC")))
                                .thenReturn("{\"events\":[{\"type\":\"STUDY\",\"subject\":\"Spring Boot\",\"eventTime\":123456}]}");

                assertThatThrownBy(() -> extractionService.extract(JOURNAL_ID))
                                .isInstanceOf(AiExtractionException.class)
                                .hasMessageContaining("ISO-8601");
                verify(lifeEventRepository, never()).deleteAll(anyList());
        }

    @Test
    void preservesUnknownOptionalFieldsAsNullAndDoesNotInventAnEventTime() {
        when(aiExtractionService.extract("I studied Spring Boot and drank water.", ENTRY_DATE, ZoneId.of("UTC")))
                .thenReturn("{\"events\":[{\"type\":\"STUDY\",\"subject\":\"Spring Boot\"}]}");

        var result = extractionService.extract(JOURNAL_ID).getFirst();

        assertThat(result.durationMinutes()).isNull();
        assertThat(result.eventTime()).isNull();
    }

    @Test
    void forcesJournalProvenanceEvenWhenModelClaimsAnotherSource() {
        when(aiExtractionService.extract("I studied Spring Boot and drank water.", ENTRY_DATE, ZoneId.of("UTC")))
                .thenReturn("{\"events\":[{\"type\":\"STUDY\",\"source\":\"DEVICE\",\"subject\":\"Spring Boot\"}]}");

        var result = extractionService.extract(JOURNAL_ID).getFirst();

        assertThat(result.source()).isEqualTo(EventSource.JOURNAL);
        assertThat(result.journalEntryId()).isEqualTo(JOURNAL_ID);
    }

    @Test
    void repeatedExtractionReplacesOnlyPreviousJournalEvents() {
        when(aiExtractionService.extract("I studied Spring Boot and drank water.", ENTRY_DATE, ZoneId.of("UTC")))
                .thenReturn("{\"events\":[{\"type\":\"STUDY\",\"subject\":\"Spring Boot\"}]}");

        extractionService.extract(JOURNAL_ID);
        extractionService.extract(JOURNAL_ID);

        verify(lifeEventRepository, times(2)).deleteAll(List.of());
        verify(lifeEventRepository, times(2)).saveAll(anyList());
    }

    @Test
    void providerFailureDoesNotModifyTheJournalOrExistingEvents() {
        when(aiExtractionService.extract("I studied Spring Boot and drank water.", ENTRY_DATE, ZoneId.of("UTC")))
                .thenThrow(new AiExtractionException("provider unavailable"));

        assertThatThrownBy(() -> extractionService.extract(JOURNAL_ID))
                .isInstanceOf(AiExtractionException.class)
                .hasMessage("provider unavailable");
        verify(journalEntryRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(journalEntryRepository, never()).delete(org.mockito.ArgumentMatchers.any());
        verify(lifeEventRepository, never()).deleteAll(anyList());
    }

    @Test
    void rejectsMissingJournalBeforeCallingTheProvider() {
        when(journalEntryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> extractionService.extract(99L))
                .isInstanceOf(JournalNotFoundException.class);
        verify(aiExtractionService, never()).extract(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsUnknownJsonFields() {
        when(aiExtractionService.extract("I studied Spring Boot and drank water.", ENTRY_DATE, ZoneId.of("UTC")))
                .thenReturn("{\"events\":[{\"type\":\"STUDY\",\"subject\":\"Spring Boot\",\"durationHours\":2}]}");

        assertThatThrownBy(() -> extractionService.extract(JOURNAL_ID))
                .isInstanceOf(AiExtractionException.class)
                .hasMessageContaining("unsupported event field");
        verify(lifeEventRepository, never()).deleteAll(anyList());
    }

    @Test
    void modelCannotChangeTheOriginalJournalContent() {
        when(aiExtractionService.extract("I studied Spring Boot and drank water.", ENTRY_DATE, ZoneId.of("UTC")))
                .thenReturn("{\"events\":[]}");
        var journal = journalEntryRepository.findById(JOURNAL_ID).orElseThrow();

        extractionService.extract(JOURNAL_ID);

        assertThat(journal.getContent()).isEqualTo("I studied Spring Boot and drank water.");
        verify(journalEntryRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
