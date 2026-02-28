package com.kartik.bankingsystem.dto;

import jakarta.validation.constraints.NotNull;

public record AccountStatusUpdateRequest(
        @NotNull(message = "active flag is required")
        Boolean active
) {
}
