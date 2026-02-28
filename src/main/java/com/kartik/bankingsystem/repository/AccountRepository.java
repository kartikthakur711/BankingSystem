package com.kartik.bankingsystem.repository;

import com.kartik.bankingsystem.entity.Account;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends MongoRepository<Account, String> {
    Optional<Account> findByAccountNumber(String accountNumber);
    boolean existsByAccountNumber(String accountNumber);
    List<Account> findAllByUserIdOrderByCreatedAtDesc(String userId);
}
