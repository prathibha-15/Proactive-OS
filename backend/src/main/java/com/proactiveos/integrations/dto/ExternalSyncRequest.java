package com.proactiveos.integrations.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ExternalSyncRequest(
        @NotNull @Size(max = 1000) List<@Valid ExternalActivityRecord> records
) {
}