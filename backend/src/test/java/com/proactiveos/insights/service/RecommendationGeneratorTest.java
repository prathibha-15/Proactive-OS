package com.proactiveos.insights.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.proactiveos.events.entity.LifeEventType;
import com.proactiveos.insights.dto.RecommendationCategory;
import com.proactiveos.insights.dto.RepeatedActivityObservation;
import org.junit.jupiter.api.Test;

class RecommendationGeneratorTest {

    private final RecommendationGenerator generator = new RecommendationGenerator();

    @Test
    void recommendsStudyTopicAtThreeDistinctDatesAndUsesObservedWording() {
        var recommendations = generator.generate(List.of(
                observation(LifeEventType.STUDY, "Spring Boot", 3, 4)), 0);

        assertThat(recommendations).hasSize(1);
        assertThat(recommendations.getFirst().id()).isEqualTo("study:spring boot");
        assertThat(recommendations.getFirst().category()).isEqualTo(RecommendationCategory.STUDY);
        assertThat(recommendations.getFirst().title()).isEqualTo("Keep your study momentum");
        assertThat(recommendations.getFirst().message()).contains("3 distinct days", "4 entries")
                .contains("Consider continuing").doesNotContain("habit");
    }

    @Test
    void doesNotRecommendStudyTopicBelowThreeDistinctDates() {
        assertThat(generator.generate(List.of(observation(LifeEventType.STUDY, "Math", 2, 5)), 0)).isEmpty();
    }

    @Test
    void recommendsWorkoutActivityAtThreeDistinctDates() {
        var recommendations = generator.generate(List.of(
                observation(LifeEventType.WORKOUT, "Legs", 3, 3)), 0);

        assertThat(recommendations).hasSize(1);
        assertThat(recommendations.getFirst().category()).isEqualTo(RecommendationCategory.WORKOUT);
        assertThat(recommendations.getFirst().message()).contains("Legs", "3 distinct days").doesNotContain("habit");
    }

    @Test
    void recommendsDataQualityOnlyWhenUntimedEventsExist() {
        var present = generator.generate(List.of(), 4);
        assertThat(present).singleElement()
                .satisfies(recommendation -> {
                    assertThat(recommendation.category()).isEqualTo(RecommendationCategory.DATA_QUALITY);
                    assertThat(recommendation.message()).contains("4 logged events", "no activity time")
                            .doesNotContain("forgot");
                });
        assertThat(generator.generate(List.of(), 0)).isEmpty();
    }

    @Test
    void deduplicatesCaseVariantsAndOrdersMultipleSubjectsDeterministically() {
        var input = List.of(
                observation(LifeEventType.STUDY, "zoology", 3, 3),
                observation(LifeEventType.STUDY, "Spring Boot", 4, 4),
                observation(LifeEventType.STUDY, "SPRING BOOT", 4, 4),
                observation(LifeEventType.WORKOUT, "Legs", 3, 3));

        var recommendations = generator.generate(input, 0);

        assertThat(recommendations).extracting("id")
                .containsExactly("study:spring boot", "study:zoology", "workout:legs");
        assertThat(recommendations.getFirst().message()).contains("SPRING BOOT");
    }

    @Test
    void ignoresUnsupportedObservationTypesAndSparseInputs() {
        var recommendations = generator.generate(List.of(
                observation(LifeEventType.WATER, "GLASS", 5, 8),
                observation(LifeEventType.STUDY, "Physics", 1, 1)), 0);
        assertThat(recommendations).isEmpty();
    }

    @Test
    void returnsNoRecommendationForNoData() {
        assertThat(generator.generate(List.of(), 0)).isEmpty();
    }

    private RepeatedActivityObservation observation(LifeEventType type, String label, long distinctDays, long eventCount) {
        return new RepeatedActivityObservation(type, label, distinctDays, eventCount);
    }
}
