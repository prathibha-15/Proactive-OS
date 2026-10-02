package com.proactiveos.insights.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.proactiveos.events.entity.LifeEventType;
import com.proactiveos.insights.dto.Recommendation;
import com.proactiveos.insights.dto.RecommendationCategory;
import com.proactiveos.insights.dto.RepeatedActivityObservation;
import org.springframework.stereotype.Component;

@Component
public class RecommendationGenerator {

    public List<Recommendation> generate(List<RepeatedActivityObservation> observations, long unknownTimeEventCount) {
        Map<String, Recommendation> unique = new LinkedHashMap<>();
        observations.stream()
                .filter(observation -> observation.distinctDays() >= ProactiveInsightsService.REPEAT_THRESHOLD_DAYS)
                .filter(observation -> observation.type() == LifeEventType.STUDY
                        || observation.type() == LifeEventType.WORKOUT)
                .sorted(Comparator.comparing((RepeatedActivityObservation observation) -> observation.type().name())
                        .thenComparing(observation -> observation.label().toLowerCase(Locale.ROOT))
                        .thenComparing(RepeatedActivityObservation::label))
                .forEach(observation -> addActivityRecommendation(unique, observation));

        if (unknownTimeEventCount > 0) {
            String id = "data-quality:unknown-event-times";
            unique.put(id, new Recommendation(
                    id,
                    RecommendationCategory.DATA_QUALITY,
                    "Some activity times are unknown",
                    "%d logged events have no activity time. This limits time-based summaries; adding a time when known can make those summaries more precise."
                            .formatted(unknownTimeEventCount)));
        }

        List<Recommendation> result = new ArrayList<>(unique.values());
        result.sort(Comparator.comparingInt((Recommendation recommendation) -> order(recommendation.category()))
                .thenComparing(Recommendation::id));
        return List.copyOf(result);
    }

    private void addActivityRecommendation(Map<String, Recommendation> recommendations,
                                           RepeatedActivityObservation observation) {
        boolean study = observation.type() == LifeEventType.STUDY;
        RecommendationCategory category = study ? RecommendationCategory.STUDY : RecommendationCategory.WORKOUT;
        String normalizedLabel = observation.label().trim().toLowerCase(Locale.ROOT);
        String id = category.name().toLowerCase(Locale.ROOT) + ":" + normalizedLabel;
        String title = study ? "Keep your study momentum" : "Keep exploring your workout activity";
        String subject = study ? "study" : "workout";
        String message = "You recorded %s activity for \"%s\" on %d distinct days in the last %d days (%d entries). "
                .formatted(subject, observation.label(), observation.distinctDays(),
                        ProactiveInsightsService.WINDOW_DAYS, observation.eventCount())
                + "Consider continuing this focus if it remains useful to you.";
        recommendations.putIfAbsent(id, new Recommendation(id, category, title, message));
    }

    private int order(RecommendationCategory category) {
        return switch (category) {
            case STUDY -> 0;
            case WORKOUT -> 1;
            case DATA_QUALITY -> 2;
        };
    }
}
