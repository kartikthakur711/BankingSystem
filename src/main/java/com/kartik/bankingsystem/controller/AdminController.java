package com.kartik.bankingsystem.controller;

import com.kartik.bankingsystem.dto.AccountResponse;
import com.kartik.bankingsystem.dto.AdminUserResponse;
import com.kartik.bankingsystem.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserResponse>> users(
            @RequestParam(required = false) String email
    ) {
        return ResponseEntity.ok(adminService.allUsers(email));
    }

    @GetMapping("/accounts")
    public ResponseEntity<List<AccountResponse>> accounts(
            @RequestParam(required = false) String accountNumber
    ) {
        return ResponseEntity.ok(adminService.allAccounts(accountNumber));
    }
}
