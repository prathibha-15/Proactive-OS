package com.proactiveos.extraction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneId;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ExtractionResponseParserTest {

    private static ValidatorFactory validationFactory;
    private ExtractionResponseParser parser;

    @BeforeAll
    static void createValidator() {
        validationFactory = Validation.buildDefaultValidatorFactory();
    }

    @BeforeEach
    void createParser() {
        parser = new ExtractionResponseParser(new ObjectMapper(), validationFactory.getValidator());
    }

    @AfterAll
    static void closeValidator() throws Exception {
        validationFactory.close();
    }

    @Test
    void convertsJournalLocalWallTimeToUtcUsingConfiguredZone() {
        var event = parse("2026-10-03T14:00:00", LocalDate.of(2026, 10, 3), ZoneId.of("Asia/Kolkata"));

        assertThat(event.eventTime()).isEqualTo(Instant.parse("2026-10-03T08:30:00Z"));
    }

    @Test
    void anchorsClockOnlyTimeToJournalDate() {
        var event = parse("14:00:00", LocalDate.of(2026, 10, 3), ZoneId.of("Asia/Kolkata"));

        assertThat(event.eventTime()).isEqualTo(Instant.parse("2026-10-03T08:30:00Z"));
    }

    @Test
    void keepsAnOffsetBearingTimestampAsTheSameInstant() {
        var event = parse("2026-10-03T14:00:00+05:30", LocalDate.of(2026, 10, 3), ZoneId.of("UTC"));

        assertThat(event.eventTime()).isEqualTo(Instant.parse("2026-10-03T08:30:00Z"));
    }

    @Test
    void rejectsInvalidAndNonexistentLocalTimestamps() {
        assertThatThrownBy(() -> parse("2026-10-03T25:00:00", LocalDate.of(2026, 10, 3), ZoneId.of("UTC")))
                .isInstanceOf(AiExtractionException.class)
                .hasMessageContaining("valid ISO-8601");
        assertThatThrownBy(() -> parse("2026-03-08T02:30:00", LocalDate.of(2026, 3, 8), ZoneId.of("America/New_York")))
                .isInstanceOf(AiExtractionException.class)
                .hasMessageContaining("nonexistent local time");
    }

    @Test
    void leavesAmbiguousDstOverlapTimeUnknown() {
        var event = parse("2026-11-01T01:30:00", LocalDate.of(2026, 11, 1), ZoneId.of("America/New_York"));

        assertThat(event.eventTime()).isNull();
    }

    @Test
    void preservesMissingTimeAsNull() {
        var event = parse(null, LocalDate.of(2026, 10, 3), ZoneId.of("Asia/Kolkata"));

        assertThat(event.eventTime()).isNull();
    }

    private com.proactiveos.events.dto.EventRequest parse(String eventTime, LocalDate date, ZoneId zoneId) {
        String timeValue = eventTime == null ? "null" : "\"" + eventTime + "\"";
        return parser.parseAndValidate("""
                {"events":[{"type":"STUDY","subject":"Spring Boot","eventTime":%s}]}
                """.formatted(timeValue), 13L, date, zoneId).getFirst();
    }

}