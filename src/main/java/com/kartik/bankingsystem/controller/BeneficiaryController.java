package com.kartik.bankingsystem.controller;

import com.kartik.bankingsystem.dto.AddBeneficiaryRequest;
import com.kartik.bankingsystem.dto.BeneficiaryResponse;
import com.kartik.bankingsystem.service.BeneficiaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/beneficiaries")
@RequiredArgsConstructor
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;

    @PostMapping
    public ResponseEntity<BeneficiaryResponse> add(@Valid @RequestBody AddBeneficiaryRequest request,
                                                   Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(beneficiaryService.add(authentication.getName(), request));
    }

    @GetMapping
    public ResponseEntity<List<BeneficiaryResponse>> list(Authentication authentication) {
        return ResponseEntity.ok(beneficiaryService.list(authentication.getName()));
    }

    @DeleteMapping("/{beneficiaryId}")
    public ResponseEntity<Void> remove(@PathVariable String beneficiaryId, Authentication authentication) {
        beneficiaryService.remove(authentication.getName(), beneficiaryId);
        return ResponseEntity.noContent().build();
    }
}
