package com.kartik.bankingsystem.repository;

import com.kartik.bankingsystem.entity.Beneficiary;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface BeneficiaryRepository extends MongoRepository<Beneficiary, String> {
    List<Beneficiary> findByUserIdOrderByCreatedAtDesc(String userId);
    Optional<Beneficiary> findByUserIdAndBeneficiaryAccountNumber(String userId, String beneficiaryAccountNumber);
    void deleteByIdAndUserId(String id, String userId);
}
