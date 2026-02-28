package com.kartik.bankingsystem.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record TransferRequest(
        @NotBlank(message = "From account number is required")
        String fromAccountNumber,
        @NotBlank(message = "To account number is required")
        String toAccountNumber,
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", inclusive = true, message = "Amount must be > 0")
        BigDecimal amount,
        @Size(max = 250, message = "Description can be at most 250 chars")
        String description,
        String idempotencyKey
) {
}
