package com.kartik.bankingsystem.dto;

import com.kartik.bankingsystem.entity.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateAccountRequest(
        @NotNull(message = "Account type is required")
        AccountType accountType,
        @NotNull(message = "Initial deposit is required")
        @DecimalMin(value = "0.00", inclusive = true, message = "Initial deposit must be >= 0")
        BigDecimal initialDeposit
) {
}
