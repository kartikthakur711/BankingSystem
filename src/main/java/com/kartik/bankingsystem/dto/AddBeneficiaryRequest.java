package com.kartik.bankingsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddBeneficiaryRequest(
        @NotBlank(message = "Nickname is required")
        @Size(max = 50, message = "Nickname can be at most 50 chars")
        String nickname,
        @NotBlank(message = "Beneficiary account number is required")
        String beneficiaryAccountNumber
) {
}
