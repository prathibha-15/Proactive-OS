package com.proactiveos.insights.dto;

public record Recommendation(
        String id,
        RecommendationCategory category,
        String title,
        String message
) {
}
