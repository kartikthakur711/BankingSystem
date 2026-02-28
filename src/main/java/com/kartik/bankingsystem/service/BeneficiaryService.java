package com.kartik.bankingsystem.service;

import com.kartik.bankingsystem.dto.AddBeneficiaryRequest;
import com.kartik.bankingsystem.dto.BeneficiaryResponse;
import com.kartik.bankingsystem.exception.UnauthorizedException;
import com.kartik.bankingsystem.repository.AccountRepository;
import com.kartik.bankingsystem.repository.BeneficiaryRepository;
import com.kartik.bankingsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    public BeneficiaryResponse add(String email, AddBeneficiaryRequest request) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        String accountNo = request.beneficiaryAccountNumber().trim().toUpperCase();
        if (accountRepository.findByAccountNumber(accountNo).isEmpty()) {
            throw new IllegalArgumentException("Beneficiary account not found");
        }

        if (beneficiaryRepository.findByUserIdAndBeneficiaryAccountNumber(user.getId(), accountNo).isPresent()) {
            throw new IllegalArgumentException("Beneficiary already exists");
        }

        var saved = beneficiaryRepository.save(
                com.kartik.bankingsystem.entity.Beneficiary.builder()
                        .userId(user.getId())
                        .nickname(request.nickname().trim())
                        .beneficiaryAccountNumber(accountNo)
                        .build()
        );

        return new BeneficiaryResponse(saved.getId(), saved.getNickname(), saved.getBeneficiaryAccountNumber(), saved.getCreatedAt());
    }

    public List<BeneficiaryResponse> list(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        return beneficiaryRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(b -> new BeneficiaryResponse(b.getId(), b.getNickname(), b.getBeneficiaryAccountNumber(), b.getCreatedAt()))
                .toList();
    }

    public void remove(String email, String beneficiaryId) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        beneficiaryRepository.deleteByIdAndUserId(beneficiaryId, user.getId());
    }

    public boolean isAllowedBeneficiary(String userId, String beneficiaryAccountNumber) {
        return beneficiaryRepository.findByUserIdAndBeneficiaryAccountNumber(userId, beneficiaryAccountNumber).isPresent();
    }
}
