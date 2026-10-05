package com.proactiveos.integrations.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import com.proactiveos.events.entity.EventSource;
import com.proactiveos.events.entity.LifeEventType;
import com.proactiveos.integrations.dto.ExternalActivityRecord;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExternalEventNormalizerTest {

    private static ValidatorFactory validatorFactory;
    private ExternalEventNormalizer normalizer;

    @BeforeAll
    static void createValidatorFactory() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @BeforeEach
    void createNormalizer() {
        normalizer = new ExternalEventNormalizer(validatorFactory.getValidator());
    }

    @Test
    void normalizesStepsIntoDeviceEventAndPreservesUnknownTime() {
        var event = normalizer.normalize(new ExternalActivityRecord(
                "steps-record", LifeEventType.STEPS, null, null, 8247, null, null));

        assertThat(event.type()).isEqualTo(LifeEventType.STEPS);
        assertThat(event.source()).isEqualTo(EventSource.DEVICE);
        assertThat(event.journalEntryId()).isNull();
        assertThat(event.count()).isEqualTo(8247);
        assertThat(event.eventTime()).isNull();
    }

    @Test
    void normalizesWorkoutAndSleepRecordsWithoutInventingFacts() {
        var workout = normalizer.normalize(new ExternalActivityRecord("workout-record", LifeEventType.WORKOUT,
                Instant.parse("2026-10-03T12:00:00Z"), 40, null, "Leg workout", null));
        var sleep = normalizer.normalize(new ExternalActivityRecord("sleep-record", LifeEventType.SLEEP,
                Instant.parse("2026-10-03T00:30:00Z"), 480, null, null, null));

        assertThat(workout.source()).isEqualTo(EventSource.DEVICE);
        assertThat(workout.activityType()).isEqualTo("Leg workout");
        assertThat(workout.durationMinutes()).isEqualTo(40);
        assertThat(sleep.source()).isEqualTo(EventSource.DEVICE);
        assertThat(sleep.eventTime()).isEqualTo(Instant.parse("2026-10-03T00:30:00Z"));
        assertThat(sleep.durationMinutes()).isEqualTo(480);
        assertThat(sleep.notes()).isNull();
    }

    @Test
    void rejectsUnsupportedTypesAndInvalidProviderFacts() {
        assertThatThrownBy(() -> normalizer.normalize(new ExternalActivityRecord(
                "food-record", LifeEventType.FOOD, Instant.now(), null, null, null, null)))
                .isInstanceOf(InvalidExternalActivityException.class)
                .hasMessageContaining("unsupported event type");
        assertThatThrownBy(() -> normalizer.normalize(new ExternalActivityRecord(
                "", LifeEventType.STEPS, null, null, -1, null, null)))
                .isInstanceOf(InvalidExternalActivityException.class);
        assertThatThrownBy(() -> normalizer.normalize(new ExternalActivityRecord(
                "workout-record", LifeEventType.WORKOUT, null, -10, null, "Leg workout", null)))
                .isInstanceOf(InvalidExternalActivityException.class)
                .hasMessageContaining("Duration must be positive");
    }
}