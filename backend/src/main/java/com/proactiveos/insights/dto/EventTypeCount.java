package com.proactiveos.insights.dto;

import com.proactiveos.events.entity.LifeEventType;

public record EventTypeCount(LifeEventType type, long count) {
}
