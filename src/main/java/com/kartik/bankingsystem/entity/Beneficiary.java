package com.kartik.bankingsystem.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "beneficiaries")
@CompoundIndex(name = "user_beneficiary_unique", def = "{'userId': 1, 'beneficiaryAccountNumber': 1}", unique = true)
public class Beneficiary {
    @Id
    private String id;

    private String userId;
    private String nickname;
    private String beneficiaryAccountNumber;

    @CreatedDate
    private Instant createdAt;
}
