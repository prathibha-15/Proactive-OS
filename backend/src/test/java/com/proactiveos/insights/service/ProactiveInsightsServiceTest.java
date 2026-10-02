package com.proactiveos.insights.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import com.proactiveos.events.entity.EventSource;
import com.proactiveos.events.entity.LifeEvent;
import com.proactiveos.events.entity.LifeEventType;
import com.proactiveos.events.entity.StudyEvent;
import com.proactiveos.events.entity.WaterEvent;
import com.proactiveos.events.entity.WaterUnit;
import com.proactiveos.events.entity.WorkoutEvent;
import com.proactiveos.events.repository.LifeEventRepository;
import com.proactiveos.insights.dto.EventTypeCount;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProactiveInsightsServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 1);

    @Mock
    private LifeEventRepository lifeEventRepository;

    @InjectMocks
    private ProactiveInsightsService service;

    @Test
    void summarizesCountsAndRepeatedStudyAndWorkoutAcrossDistinctDays() {
        List<LifeEvent> events = List.of(
                study("Spring Boot", TODAY.minusDays(1), 60),
                study("spring boot", TODAY.minusDays(2), 90),
                study("Spring Boot", TODAY.minusDays(2), 30),
                study("Spring Boot", TODAY.minusDays(4), 45),
                workout("Legs", TODAY.minusDays(1)),
                workout("Legs", TODAY.minusDays(5)),
                workout("Legs", TODAY.minusDays(8)),
                water(TODAY.minusDays(3), 2));
        when(lifeEventRepository.findAllByEventTimeGreaterThanEqualAndEventTimeLessThan(
                TODAY.minusDays(27).atStartOfDay(ZoneOffset.UTC).toInstant(),
                TODAY.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant())).thenReturn(events);
        when(lifeEventRepository.countByEventTimeIsNull()).thenReturn(2L);

        var response = service.getInsights(TODAY);

        assertThat(response.windowStart()).isEqualTo(TODAY.minusDays(27));
        assertThat(response.windowEnd()).isEqualTo(TODAY);
        assertThat(response.knownTimeEventCount()).isEqualTo(8);
        assertThat(response.unknownTimeEventCount()).isEqualTo(2);
        assertThat(response.eventCounts()).contains(new EventTypeCount(LifeEventType.STUDY, 4))
                .contains(new EventTypeCount(LifeEventType.WORKOUT, 3))
                .contains(new EventTypeCount(LifeEventType.WATER, 1));
        assertThat(response.repeatedActivities()).hasSize(2);
        assertThat(response.repeatedActivities())
                .anySatisfy(observation -> {
                    assertThat(observation.type()).isEqualTo(LifeEventType.STUDY);
                    assertThat(observation.label()).isEqualTo("Spring Boot");
                    assertThat(observation.distinctDays()).isEqualTo(3);
                    assertThat(observation.eventCount()).isEqualTo(4);
                })
                .anySatisfy(observation -> {
                    assertThat(observation.type()).isEqualTo(LifeEventType.WORKOUT);
                    assertThat(observation.label()).isEqualTo("Legs");
                    assertThat(observation.distinctDays()).isEqualTo(3);
                });
    }

    @Test
    void sparseOccurrencesDoNotProduceARepeatedActivityObservation() {
        when(lifeEventRepository.findAllByEventTimeGreaterThanEqualAndEventTimeLessThan(
                TODAY.minusDays(27).atStartOfDay(ZoneOffset.UTC).toInstant(),
                TODAY.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()))
                .thenReturn(List.of(study("Spring Boot", TODAY.minusDays(1), 30),
                        study("Spring Boot", TODAY.minusDays(1), 60)));
        when(lifeEventRepository.countByEventTimeIsNull()).thenReturn(0L);

        var response = service.getInsights(TODAY);

        assertThat(response.knownTimeEventCount()).isEqualTo(2);
        assertThat(response.repeatedActivities()).isEmpty();
    }

    @Test
    void unknownEventTimesAreNotAssignedToTheWindowOrPatternDates() {
        when(lifeEventRepository.findAllByEventTimeGreaterThanEqualAndEventTimeLessThan(
                TODAY.minusDays(27).atStartOfDay(ZoneOffset.UTC).toInstant(),
                TODAY.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()))
                .thenReturn(List.of(study("Spring Boot", TODAY.minusDays(1), 60)));
        when(lifeEventRepository.countByEventTimeIsNull()).thenReturn(7L);

        var response = service.getInsights(TODAY);

        assertThat(response.knownTimeEventCount()).isEqualTo(1);
        assertThat(response.unknownTimeEventCount()).isEqualTo(7);
        assertThat(response.repeatedActivities()).isEmpty();
    }

    @Test
    void multipleEventTypesAreCountedIndependently() {
        when(lifeEventRepository.findAllByEventTimeGreaterThanEqualAndEventTimeLessThan(
                TODAY.minusDays(27).atStartOfDay(ZoneOffset.UTC).toInstant(),
                TODAY.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()))
                .thenReturn(List.of(study("Math", TODAY.minusDays(1), 40), water(TODAY.minusDays(1), 1),
                        workout("Walk", TODAY.minusDays(1))));
        when(lifeEventRepository.countByEventTimeIsNull()).thenReturn(0L);

        var response = service.getInsights(TODAY);

        assertThat(response.eventCounts()).filteredOn(count -> count.count() > 0).containsExactlyInAnyOrder(
                new EventTypeCount(LifeEventType.STUDY, 1),
                new EventTypeCount(LifeEventType.WATER, 1),
                new EventTypeCount(LifeEventType.WORKOUT, 1));
    }

    @Test
    void returnsZeroCountsAndNoObservationsWhenThereIsNoData() {
        when(lifeEventRepository.findAllByEventTimeGreaterThanEqualAndEventTimeLessThan(
                TODAY.minusDays(27).atStartOfDay(ZoneOffset.UTC).toInstant(),
                TODAY.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant())).thenReturn(List.of());
        when(lifeEventRepository.countByEventTimeIsNull()).thenReturn(0L);

        var response = service.getInsights(TODAY);

        assertThat(response.knownTimeEventCount()).isZero();
        assertThat(response.unknownTimeEventCount()).isZero();
        assertThat(response.eventCounts()).hasSize(LifeEventType.values().length)
                .allSatisfy(count -> assertThat(count.count()).isZero());
        assertThat(response.repeatedActivities()).isEmpty();
    }

    private StudyEvent study(String subject, LocalDate date, int durationMinutes) {
        return new StudyEvent(null, date.atTime(12, 0).toInstant(ZoneOffset.UTC), EventSource.JOURNAL,
                null, subject, durationMinutes);
    }

    private WorkoutEvent workout(String activity, LocalDate date) {
        return new WorkoutEvent(null, date.atTime(12, 0).toInstant(ZoneOffset.UTC), EventSource.MANUAL,
                null, activity, null);
    }

    private WaterEvent water(LocalDate date, int quantity) {
        return new WaterEvent(null, date.atTime(12, 0).toInstant(ZoneOffset.UTC), EventSource.DEVICE,
                null, quantity, WaterUnit.GLASS);
    }
}
