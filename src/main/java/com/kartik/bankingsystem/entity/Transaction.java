package com.kartik.bankingsystem.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "transactions")
public class Transaction {
    @Id
    private String id;

    @Indexed
    private String accountId;

    @Indexed
    private String userId;

    private TransactionType type;

    private BigDecimal amount;

    private BigDecimal balanceAfter;

    private String counterpartyAccountNumber;

    private String description;

    @Indexed(unique = true, sparse = true)
    private String idempotencyKey;

    @CreatedDate
    private Instant createdAt;
}
