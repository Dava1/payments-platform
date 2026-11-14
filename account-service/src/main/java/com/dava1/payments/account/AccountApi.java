package com.dava1.payments.account;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public final class AccountApi {
    private AccountApi() {
    }

    public record CreateAccountRequest(
            @NotBlank String ownerId,
            @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency) {
    }

    public record AccountResponse(UUID id, String ownerId, String currency, long balanceMinor) {
        static AccountResponse from(Account account) {
            return new AccountResponse(account.getId(), account.getOwnerId(),
                    account.getCurrency(), account.getBalanceMinor());
        }
    }

    public record CreateTransferRequest(
            @NotNull UUID sourceAccountId,
            @NotNull UUID targetAccountId,
            @Min(1) long amountMinor,
            @NotBlank String reference) {
    }
}
