package com.dava1.payments.account;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {

    public enum Direction { DEBIT, CREDIT }

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID accountId;

    @Column(nullable = false)
    private UUID transferId;

    @Column(nullable = false)
    private long amountMinor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Direction direction;

    @Column(nullable = false)
    private Instant createdAt;

    protected LedgerEntry() {
    }

    LedgerEntry(UUID accountId, UUID transferId, long amountMinor, Direction direction) {
        this.id = UUID.randomUUID();
        this.accountId = accountId;
        this.transferId = transferId;
        this.amountMinor = amountMinor;
        this.direction = direction;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getAccountId() { return accountId; }
    public UUID getTransferId() { return transferId; }
    public long getAmountMinor() { return amountMinor; }
    public Direction getDirection() { return direction; }
    public Instant getCreatedAt() { return createdAt; }
}
