package com.dava1.payments.account;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1")
public class AccountController {

    private final AccountOperations operations;

    public AccountController(AccountOperations operations) {
        this.operations = operations;
    }

    @PostMapping("/accounts")
    public ResponseEntity<AccountApi.AccountResponse> createAccount(
            @Valid @RequestBody AccountApi.CreateAccountRequest request) {
        var account = operations.open(request);
        return ResponseEntity.created(URI.create("/api/v1/accounts/" + account.id())).body(account);
    }

    @GetMapping("/accounts/{id}")
    public AccountApi.AccountResponse getAccount(@PathVariable UUID id) {
        return operations.account(id);
    }

    @GetMapping("/accounts/{id}/entries")
    public Page<LedgerEntry> getEntries(@PathVariable UUID id,
                                        @RequestParam(defaultValue = "0") @Min(0) int page,
                                        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return operations.entries(id, page, size);
    }

    @PostMapping("/transfers")
    public ResponseEntity<Transfer> createTransfer(
            @Valid @RequestBody AccountApi.CreateTransferRequest request) {
        var transfer = operations.transfer(request);
        return ResponseEntity.created(URI.create("/api/v1/transfers/" + transfer.getId())).body(transfer);
    }

    @GetMapping("/transfers/{id}")
    public Transfer getTransfer(@PathVariable UUID id) {
        return operations.getTransfer(id);
    }
}
