package com.kartik.bankingsystem.dto;

import java.time.Instant;

public record BeneficiaryResponse(
        String id,
        String nickname,
        String beneficiaryAccountNumber,
        Instant createdAt
) {
}
