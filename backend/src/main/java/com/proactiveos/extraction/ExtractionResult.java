package com.proactiveos.extraction;

import java.util.List;

import com.proactiveos.events.dto.EventRequest;

public record ExtractionResult(List<EventRequest> events) {
}
