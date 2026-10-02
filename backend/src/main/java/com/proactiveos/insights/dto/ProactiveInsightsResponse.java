package com.proactiveos.insights.dto;

import java.time.LocalDate;
import java.util.List;

public record ProactiveInsightsResponse(
        LocalDate windowStart,
        LocalDate windowEnd,
        long knownTimeEventCount,
        long unknownTimeEventCount,
        List<EventTypeCount> eventCounts,
        List<RepeatedActivityObservation> repeatedActivities,
        List<Recommendation> recommendations
) {
}
