package com.kartik.bankingsystem.service;

import com.kartik.bankingsystem.dto.AccountResponse;
import com.kartik.bankingsystem.dto.AdminUserResponse;
import com.kartik.bankingsystem.repository.AccountRepository;
import com.kartik.bankingsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    public List<AdminUserResponse> allUsers(String emailQuery) {
        String query = emailQuery == null ? "" : emailQuery.trim().toLowerCase(Locale.ROOT);
        return userRepository.findAll().stream()
                .filter(u -> query.isEmpty() || u.getEmail().toLowerCase(Locale.ROOT).contains(query))
                .map(u -> new AdminUserResponse(u.getId(), u.getFullName(), u.getEmail(), u.getRoles(), u.isActive()))
                .toList();
    }

    public List<AccountResponse> allAccounts(String accountNumberQuery) {
        String query = accountNumberQuery == null ? "" : accountNumberQuery.trim().toUpperCase(Locale.ROOT);
        return accountRepository.findAll().stream()
                .filter(a -> query.isEmpty() || a.getAccountNumber().contains(query))
                .map(a -> new AccountResponse(
                        a.getAccountNumber(),
                        a.getAccountType(),
                        a.getBalance(),
                        a.getCurrency(),
                        a.isActive()
                ))
                .toList();
    }
}
