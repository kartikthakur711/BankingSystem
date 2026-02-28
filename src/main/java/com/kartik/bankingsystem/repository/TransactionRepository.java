package com.kartik.bankingsystem.repository;

import com.kartik.bankingsystem.entity.Transaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends MongoRepository<Transaction, String> {
    List<Transaction> findByAccountIdOrderByCreatedAtDesc(String accountId, Pageable pageable);
    List<Transaction> findByAccountIdAndCreatedAtBetweenOrderByCreatedAtDesc(String accountId, Instant from, Instant to);
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
}
