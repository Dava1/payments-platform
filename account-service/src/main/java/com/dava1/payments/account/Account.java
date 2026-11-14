package com.dava1.payments.account;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String ownerId;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private long balanceMinor;

    @Version
    private long version;

    protected Account() {
    }

    Account(String ownerId, String currency) {
        this.id = UUID.randomUUID();
        this.ownerId = ownerId;
        this.currency = currency;
    }

    public UUID getId() { return id; }
    public String getOwnerId() { return ownerId; }
    public String getCurrency() { return currency; }
    public long getBalanceMinor() { return balanceMinor; }

    void adjustBalance(long amountMinor) {
        balanceMinor = Math.addExact(balanceMinor, amountMinor);
        if (balanceMinor < 0) {
            throw new IllegalArgumentException("Insufficient funds");
        }
    }
}
