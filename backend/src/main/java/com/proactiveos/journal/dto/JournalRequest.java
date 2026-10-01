package com.proactiveos.journal.dto;

import jakarta.validation.constraints.NotBlank;

public record JournalRequest(@NotBlank(message = "Content must not be blank.") String content) {
}