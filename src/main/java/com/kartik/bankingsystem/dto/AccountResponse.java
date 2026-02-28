package com.kartik.bankingsystem.dto;

import com.kartik.bankingsystem.entity.AccountType;

import java.math.BigDecimal;

public record AccountResponse(
        String accountNumber,
        AccountType accountType,
        BigDecimal balance,
        String currency,
        boolean active
) {
}
