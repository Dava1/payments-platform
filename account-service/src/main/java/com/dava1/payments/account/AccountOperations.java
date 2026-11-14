package com.dava1.payments.account;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountOperations {

    private final AccountRepository accounts;
    private final TransferRepository transfers;
    private final LedgerEntryRepository entries;

    public AccountOperations(AccountRepository accounts, TransferRepository transfers,
                             LedgerEntryRepository entries) {
        this.accounts = accounts;
        this.transfers = transfers;
        this.entries = entries;
    }

    @Transactional
    public AccountApi.AccountResponse open(AccountApi.CreateAccountRequest request) {
        return AccountApi.AccountResponse.from(
                accounts.save(new Account(request.ownerId(), request.currency())));
    }

    @Transactional(readOnly = true)
    public AccountApi.AccountResponse account(UUID id) {
        return AccountApi.AccountResponse.from(findAccount(id));
    }

    @Transactional(readOnly = true)
    public Page<LedgerEntry> entries(UUID accountId, int page, int size) {
        findAccount(accountId);
        return entries.findByAccountId(accountId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Transactional(readOnly = true)
    public Transfer getTransfer(UUID id) {
        return transfers.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transfer not found"));
    }

    @Transactional
    public Transfer transfer(AccountApi.CreateTransferRequest request) {
        if (request.sourceAccountId().equals(request.targetAccountId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Accounts must be different");
        }
        if (request.amountMinor() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be positive");
        }

        // A stable lock order prevents deadlocks when transfers go in opposite directions.
        UUID firstId = request.sourceAccountId().compareTo(request.targetAccountId()) < 0
                ? request.sourceAccountId() : request.targetAccountId();
        UUID secondId = firstId.equals(request.sourceAccountId())
                ? request.targetAccountId() : request.sourceAccountId();

        Account first = lockAccount(firstId);
        Account second = lockAccount(secondId);
        Account source = first.getId().equals(request.sourceAccountId()) ? first : second;
        Account target = first.getId().equals(request.targetAccountId()) ? first : second;

        Transfer previous = transfers.findByReference(request.reference()).orElse(null);
        if (previous != null) {
            if (previous.getSourceAccountId().equals(request.sourceAccountId())
                    && previous.getTargetAccountId().equals(request.targetAccountId())
                    && previous.getAmountMinor() == request.amountMinor()) {
                return previous;
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reference already used for a different transfer");
        }

        if (!source.getCurrency().equals(target.getCurrency())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Currency mismatch");
        }
        if (source.getBalanceMinor() < request.amountMinor()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Insufficient funds");
        }

        source.adjustBalance(-request.amountMinor());
        try {
            target.adjustBalance(request.amountMinor());
        } catch (ArithmeticException overflow) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Target balance overflow");
        }

        Transfer transfer = transfers.save(new Transfer(
                source.getId(), target.getId(), request.amountMinor(), request.reference()));

        entries.save(new LedgerEntry(source.getId(), transfer.getId(), request.amountMinor(),
                LedgerEntry.Direction.DEBIT));
        entries.save(new LedgerEntry(target.getId(), transfer.getId(), request.amountMinor(),
                LedgerEntry.Direction.CREDIT));

        return transfer;
    }

    private Account findAccount(UUID id) {
        return accounts.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
    }

    private Account lockAccount(UUID id) {
        return accounts.findLocked(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
    }
}
