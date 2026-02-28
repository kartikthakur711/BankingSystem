package com.kartik.bankingsystem.controller;

import com.kartik.bankingsystem.dto.*;
import com.kartik.bankingsystem.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request,
                                                         Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountService.createAccount(authentication.getName(), request));
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> myAccounts(Authentication authentication) {
        return ResponseEntity.ok(accountService.myAccounts(authentication.getName()));
    }

    @PostMapping("/{accountNumber}/deposit")
    public ResponseEntity<AccountResponse> deposit(@PathVariable String accountNumber,
                                                   @Valid @RequestBody AmountRequest request,
                                                   Authentication authentication) {
        return ResponseEntity.ok(accountService.deposit(authentication.getName(), accountNumber, request));
    }

    @PostMapping("/{accountNumber}/withdraw")
    public ResponseEntity<AccountResponse> withdraw(@PathVariable String accountNumber,
                                                    @Valid @RequestBody AmountRequest request,
                                                    Authentication authentication) {
        return ResponseEntity.ok(accountService.withdraw(authentication.getName(), accountNumber, request));
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest request,
                                                     Authentication authentication) {
        return ResponseEntity.ok(accountService.transfer(authentication.getName(), request));
    }

    @GetMapping("/{accountNumber}/transactions")
    public ResponseEntity<List<TransactionResponse>> transactionHistory(@PathVariable String accountNumber,
                                                                        @RequestParam(defaultValue = "0") int page,
                                                                        @RequestParam(defaultValue = "20") int size,
                                                                        Authentication authentication) {
        return ResponseEntity.ok(accountService.transactionHistory(authentication.getName(), accountNumber, page, size));
    }

    @GetMapping(value = "/{accountNumber}/statement", produces = "text/csv")
    public ResponseEntity<String> monthlyStatement(@PathVariable String accountNumber,
                                                   @RequestParam int year,
                                                   @RequestParam int month,
                                                   Authentication authentication) {
        String csv = accountService.monthlyStatementCsv(authentication.getName(), accountNumber, year, month);
        String filename = "statement-" + accountNumber + "-" + year + "-" + month + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @GetMapping(value = "/{accountNumber}/statement/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> monthlyStatementPdf(@PathVariable String accountNumber,
                                                      @RequestParam int year,
                                                      @RequestParam int month,
                                                      Authentication authentication) {
        byte[] pdf = accountService.monthlyStatementPdf(authentication.getName(), accountNumber, year, month);
        String filename = "statement-" + accountNumber + "-" + year + "-" + month + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PatchMapping("/{accountNumber}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountResponse> updateAccountStatus(@PathVariable String accountNumber,
                                                               @Valid @RequestBody AccountStatusUpdateRequest request) {
        return ResponseEntity.ok(accountService.updateAccountStatus(accountNumber, request.active()));
    }
}
