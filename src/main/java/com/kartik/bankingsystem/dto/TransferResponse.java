package com.kartik.bankingsystem.dto;

import java.math.BigDecimal;

public record TransferResponse(
        String fromAccountNumber,
        String toAccountNumber,
        BigDecimal amount,
        String message
) {
}
