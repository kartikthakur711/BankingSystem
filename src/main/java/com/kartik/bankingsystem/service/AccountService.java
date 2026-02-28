package com.kartik.bankingsystem.service;

import com.kartik.bankingsystem.dto.*;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.kartik.bankingsystem.entity.Account;
import com.kartik.bankingsystem.entity.Transaction;
import com.kartik.bankingsystem.entity.TransactionType;
import com.kartik.bankingsystem.exception.UnauthorizedException;
import com.kartik.bankingsystem.repository.AccountRepository;
import com.kartik.bankingsystem.repository.TransactionRepository;
import com.kartik.bankingsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final BeneficiaryService beneficiaryService;
    private final NotificationService notificationService;

    public AccountResponse createAccount(String email, CreateAccountRequest request) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        BigDecimal initialDeposit = request.initialDeposit();
        if (initialDeposit == null || initialDeposit.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Initial deposit must be >= 0");
        }

        Account account = Account.builder()
                .userId(user.getId())
                .accountNumber(generateUniqueAccountNumber())
                .accountType(request.accountType())
                .balance(initialDeposit)
                .currency("INR")
                .active(true)
                .build();

        Account saved = accountRepository.save(account);

        if (saved.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            recordTransaction(saved, user.getId(), TransactionType.DEPOSIT, saved.getBalance(), null,
                    "Initial deposit", null);
        }

        return toAccountResponse(saved);
    }

    public List<AccountResponse> myAccounts(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        return accountRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toAccountResponse)
                .toList();
    }

    @Transactional
    public AccountResponse deposit(String email, String accountNumber, AmountRequest request) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        Account account = findUserAccount(user.getId(), accountNumber);

        BigDecimal amount = safeAmount(request.amount());
        account.setBalance(account.getBalance().add(amount));
        Account saved = accountRepository.save(account);

        recordTransaction(saved, user.getId(), TransactionType.DEPOSIT, amount, null, request.description(), null);
        return toAccountResponse(saved);
    }

    @Transactional
    public AccountResponse withdraw(String email, String accountNumber, AmountRequest request) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        Account account = findUserAccount(user.getId(), accountNumber);

        BigDecimal amount = safeAmount(request.amount());
        ensureSufficientBalance(account, amount);

        account.setBalance(account.getBalance().subtract(amount));
        Account saved = accountRepository.save(account);

        recordTransaction(saved, user.getId(), TransactionType.WITHDRAW, amount, null, request.description(), null);
        return toAccountResponse(saved);
    }

    @Transactional
    public TransferResponse transfer(String email, TransferRequest request) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        String fromAccNo = normalize(request.fromAccountNumber());
        String toAccNo = normalize(request.toAccountNumber());

        if (fromAccNo.equals(toAccNo)) {
            throw new IllegalArgumentException("From and to account cannot be same");
        }

        if (request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
            var existing = transactionRepository.findByIdempotencyKey(request.idempotencyKey());
            if (existing.isPresent()) {
                Transaction tx = existing.get();
                if (tx.getType() == TransactionType.TRANSFER_OUT) {
                    return new TransferResponse(fromAccNo, toAccNo, request.amount(), "Transfer already processed");
                }
                throw new IllegalArgumentException("Idempotency key already used by a different transaction");
            }
        }

        Account fromAccount = findUserAccount(user.getId(), fromAccNo);
        Account toAccount = accountRepository.findByAccountNumber(toAccNo)
                .orElseThrow(() -> new IllegalArgumentException("Destination account not found"));

        if (!beneficiaryService.isAllowedBeneficiary(user.getId(), toAccNo)) {
            throw new IllegalArgumentException("Destination account is not in your beneficiary list");
        }

        BigDecimal amount = safeAmount(request.amount());
        ensureSufficientBalance(fromAccount, amount);

        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        recordTransaction(fromAccount, user.getId(), TransactionType.TRANSFER_OUT, amount,
                toAccNo, request.description(), request.idempotencyKey());
        recordTransaction(toAccount, toAccount.getUserId(), TransactionType.TRANSFER_IN, amount,
                fromAccNo, request.description(), request.idempotencyKey());

        notificationService.create(user.getId(), "Transfer Success",
                "Transferred " + amount + " from " + fromAccNo + " to " + toAccNo);
        notificationService.create(toAccount.getUserId(), "Amount Received",
                "Received " + amount + " from " + fromAccNo + " into " + toAccNo);

        return new TransferResponse(fromAccNo, toAccNo, amount, "Transfer successful");
    }

    public AccountResponse updateAccountStatus(String accountNumber, boolean active) {
        Account account = accountRepository.findByAccountNumber(normalize(accountNumber))
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        account.setActive(active);
        Account saved = accountRepository.save(account);
        return toAccountResponse(saved);
    }

    public List<TransactionResponse> transactionHistory(String email, String accountNumber, int page, int size) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        Account account = findUserAccount(user.getId(), accountNumber);

        return transactionRepository.findByAccountIdOrderByCreatedAtDesc(account.getId(), PageRequest.of(page, size))
                .stream()
                .map(this::toTransactionResponse)
                .toList();
    }

    public String monthlyStatementCsv(String email, String accountNumber, int year, int month) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        Account account = findUserAccount(user.getId(), accountNumber);

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1);
        Instant from = startDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = endDate.atStartOfDay().toInstant(ZoneOffset.UTC);

        List<Transaction> txns = transactionRepository
                .findByAccountIdAndCreatedAtBetweenOrderByCreatedAtDesc(account.getId(), from, to);

        StringBuilder csv = new StringBuilder();
        csv.append("transactionId,type,amount,balanceAfter,counterparty,description,createdAt\n");
        DateTimeFormatter fmt = DateTimeFormatter.ISO_INSTANT;
        for (Transaction t : txns) {
            csv.append(safeCsv(t.getId())).append(",")
                    .append(safeCsv(t.getType() == null ? "" : t.getType().name())).append(",")
                    .append(safeCsv(t.getAmount() == null ? "" : t.getAmount().toPlainString())).append(",")
                    .append(safeCsv(t.getBalanceAfter() == null ? "" : t.getBalanceAfter().toPlainString())).append(",")
                    .append(safeCsv(t.getCounterpartyAccountNumber())).append(",")
                    .append(safeCsv(t.getDescription())).append(",")
                    .append(safeCsv(t.getCreatedAt() == null ? "" : fmt.format(t.getCreatedAt())))
                    .append("\n");
        }
        return csv.toString();
    }

    public byte[] monthlyStatementPdf(String email, String accountNumber, int year, int month) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        Account account = findUserAccount(user.getId(), accountNumber);

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1);
        Instant from = startDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = endDate.atStartOfDay().toInstant(ZoneOffset.UTC);

        List<Transaction> txns = transactionRepository
                .findByAccountIdAndCreatedAtBetweenOrderByCreatedAtDesc(account.getId(), from, to);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            Font header = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            document.add(new Paragraph("Monthly Statement", header));
            document.add(new Paragraph("Account: " + account.getAccountNumber()));
            document.add(new Paragraph("Period: " + year + "-" + String.format("%02d", month)));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            addHeaderCell(table, "Date");
            addHeaderCell(table, "Type");
            addHeaderCell(table, "Amount");
            addHeaderCell(table, "Balance After");
            addHeaderCell(table, "Description");

            DateTimeFormatter fmt = DateTimeFormatter.ISO_INSTANT;
            for (Transaction t : txns) {
                table.addCell(text(t.getCreatedAt() == null ? "" : fmt.format(t.getCreatedAt())));
                table.addCell(text(t.getType() == null ? "" : t.getType().name()));
                table.addCell(text(t.getAmount() == null ? "" : t.getAmount().toPlainString()));
                table.addCell(text(t.getBalanceAfter() == null ? "" : t.getBalanceAfter().toPlainString()));
                table.addCell(text(t.getDescription()));
            }
            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (DocumentException | java.io.IOException ex) {
            throw new IllegalStateException("Failed to generate PDF statement", ex);
        }
    }

    private void recordTransaction(Account account,
                                   String userId,
                                   TransactionType type,
                                   BigDecimal amount,
                                   String counterparty,
                                   String description,
                                   String idempotencyKey) {
        Transaction txn = Transaction.builder()
                .accountId(account.getId())
                .userId(userId)
                .type(type)
                .amount(amount)
                .balanceAfter(account.getBalance())
                .counterpartyAccountNumber(counterparty)
                .description(description)
                .idempotencyKey(idempotencyKey)
                .build();
        transactionRepository.save(txn);

        if (type == TransactionType.DEPOSIT || type == TransactionType.WITHDRAW) {
            notificationService.create(userId, "Transaction " + type.name(),
                    type.name() + " of " + amount + " in account " + account.getAccountNumber());
        }
    }

    private Account findUserAccount(String userId, String accountNumber) {
        Account account = accountRepository.findByAccountNumber(normalize(accountNumber))
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        if (!account.getUserId().equals(userId)) {
            throw new UnauthorizedException("Account does not belong to current user");
        }
        if (!account.isActive()) {
            throw new IllegalArgumentException("Account is inactive");
        }
        return account;
    }

    private AccountResponse toAccountResponse(Account account) {
        return new AccountResponse(
                account.getAccountNumber(),
                account.getAccountType(),
                account.getBalance(),
                account.getCurrency(),
                account.isActive()
        );
    }

    private TransactionResponse toTransactionResponse(Transaction txn) {
        return new TransactionResponse(
                txn.getId(),
                txn.getType(),
                txn.getAmount(),
                txn.getBalanceAfter(),
                txn.getCounterpartyAccountNumber(),
                txn.getDescription(),
                txn.getCreatedAt()
        );
    }

    private String generateUniqueAccountNumber() {
        String value;
        do {
            long suffix = 1000000000L + Math.abs(RANDOM.nextLong() % 9000000000L);
            value = "AC" + suffix;
        } while (accountRepository.existsByAccountNumber(value));
        return value;
    }

    private String normalize(String accountNumber) {
        return accountNumber.trim().toUpperCase();
    }

    private BigDecimal safeAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }
        return amount;
    }

    private void ensureSufficientBalance(Account account, BigDecimal amount) {
        if (account.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }
    }

    private String safeCsv(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private void addHeaderCell(PdfPTable table, String value) {
        PdfPCell headerCell = new PdfPCell();
        headerCell.setPhrase(new Paragraph(value, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        table.addCell(headerCell);
    }

    private String text(String value) {
        return value == null ? "" : value;
    }
}
