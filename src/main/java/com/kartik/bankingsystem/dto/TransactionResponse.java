package com.kartik.bankingsystem.dto;

import com.kartik.bankingsystem.entity.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(
        String id,
        TransactionType type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String counterpartyAccountNumber,
        String description,
        Instant createdAt
) {
}
