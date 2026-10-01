package com.proactiveos.journal.dto;

import java.time.Instant;
import java.time.LocalDate;

public record JournalResponse(
        Long id,
        String content,
        LocalDate entryDate,
        Instant createdAt,
        Instant updatedAt
) {
}