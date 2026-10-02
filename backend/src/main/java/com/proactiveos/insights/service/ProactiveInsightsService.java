package com.proactiveos.insights.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.proactiveos.events.entity.LifeEvent;
import com.proactiveos.events.entity.LifeEventType;
import com.proactiveos.events.entity.StudyEvent;
import com.proactiveos.events.entity.WorkoutEvent;
import com.proactiveos.events.repository.LifeEventRepository;
import com.proactiveos.insights.dto.EventTypeCount;
import com.proactiveos.insights.dto.ProactiveInsightsResponse;
import com.proactiveos.insights.dto.Recommendation;
import com.proactiveos.insights.dto.RepeatedActivityObservation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProactiveInsightsService {

    static final int WINDOW_DAYS = 28;
    static final int REPEAT_THRESHOLD_DAYS = 3;

    private final LifeEventRepository lifeEventRepository;
    private final RecommendationGenerator recommendationGenerator;

    public ProactiveInsightsService(LifeEventRepository lifeEventRepository,
                                    RecommendationGenerator recommendationGenerator) {
        this.lifeEventRepository = lifeEventRepository;
        this.recommendationGenerator = recommendationGenerator;
    }

    public ProactiveInsightsResponse getInsights() {
        return getInsights(LocalDate.now(ZoneOffset.UTC));
    }

    ProactiveInsightsResponse getInsights(LocalDate todayUtc) {
        LocalDate windowStart = todayUtc.minusDays(WINDOW_DAYS - 1L);
        Instant startInclusive = windowStart.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endExclusive = todayUtc.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        List<LifeEvent> timedEvents = lifeEventRepository
                .findAllByEventTimeGreaterThanEqualAndEventTimeLessThan(startInclusive, endExclusive);

        Map<LifeEventType, Long> counts = new EnumMap<>(LifeEventType.class);
        for (LifeEventType type : LifeEventType.values()) {
            counts.put(type, 0L);
        }
        timedEvents.forEach(event -> counts.compute(event.getType(), (type, count) -> count + 1));
        List<EventTypeCount> eventCounts = counts.entrySet().stream()
                .map(entry -> new EventTypeCount(entry.getKey(), entry.getValue()))
                .toList();

        List<RepeatedActivityObservation> repeatedActivities = findRepeatedActivities(timedEvents);
        long unknownTimeEventCount = lifeEventRepository.countByEventTimeIsNull();
        List<Recommendation> recommendations = recommendationGenerator.generate(repeatedActivities, unknownTimeEventCount);
        return new ProactiveInsightsResponse(
                windowStart,
                todayUtc,
                timedEvents.size(),
            unknownTimeEventCount,
                eventCounts,
            repeatedActivities,
            recommendations);
    }

    private List<RepeatedActivityObservation> findRepeatedActivities(List<LifeEvent> events) {
        Map<ActivityKey, ActivityAccumulator> activities = new HashMap<>();
        for (LifeEvent event : events) {
            ActivityKey key = activityKey(event);
            if (key == null || event.getEventTime() == null) {
                continue;
            }
            ActivityAccumulator accumulator = activities.computeIfAbsent(key, ignored -> new ActivityAccumulator());
            accumulator.displayLabel = preferredDisplayLabel(accumulator.displayLabel, activityLabel(event).trim());
            accumulator.eventCount++;
            accumulator.days.add(event.getEventTime().atZone(ZoneOffset.UTC).toLocalDate());
        }

        List<RepeatedActivityObservation> observations = new ArrayList<>();
        activities.forEach((key, accumulator) -> {
            if (accumulator.days.size() >= REPEAT_THRESHOLD_DAYS) {
                observations.add(new RepeatedActivityObservation(
                    key.type(), accumulator.displayLabel, accumulator.days.size(), accumulator.eventCount));
            }
        });
        observations.sort(Comparator.comparingLong(RepeatedActivityObservation::distinctDays).reversed()
                .thenComparing(RepeatedActivityObservation::type)
                .thenComparing(RepeatedActivityObservation::label));
        return List.copyOf(observations);
    }

    private ActivityKey activityKey(LifeEvent event) {
        if (event instanceof StudyEvent study) {
            return keyFor(LifeEventType.STUDY, study.getSubject());
        }
        if (event instanceof WorkoutEvent workout) {
            return keyFor(LifeEventType.WORKOUT, workout.getActivityType());
        }
        return null;
    }

    private ActivityKey keyFor(LifeEventType type, String label) {
        if (label == null || label.isBlank()) {
            return null;
        }
        String displayLabel = label.trim();
        return new ActivityKey(type, displayLabel.toLowerCase(Locale.ROOT));
    }

    private String preferredDisplayLabel(String current, String candidate) {
        if (current == null) {
            return candidate;
        }
        int caseInsensitiveOrder = String.CASE_INSENSITIVE_ORDER.compare(candidate, current);
        if (caseInsensitiveOrder < 0 || (caseInsensitiveOrder == 0 && candidate.compareTo(current) < 0)) {
            return candidate;
        }
        return current;
    }

    private String activityLabel(LifeEvent event) {
        if (event instanceof StudyEvent study) {
            return study.getSubject();
        }
        if (event instanceof WorkoutEvent workout) {
            return workout.getActivityType();
        }
        throw new IllegalArgumentException("No repeated activity label for " + event.getType());
    }

    private record ActivityKey(LifeEventType type, String normalizedLabel) {
    }

    private static final class ActivityAccumulator {
        private final Set<LocalDate> days = new HashSet<>();
        private String displayLabel;
        private long eventCount;
    }
}
