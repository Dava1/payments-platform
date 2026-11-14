package com.dava1.payments.account;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "transfers", uniqueConstraints = @UniqueConstraint(columnNames = "reference"))
public class Transfer {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID sourceAccountId;

    @Column(nullable = false)
    private UUID targetAccountId;

    @Column(nullable = false)
    private long amountMinor;

    @Column(nullable = false, unique = true)
    private String reference;

    @Column(nullable = false)
    private Instant createdAt;

    protected Transfer() {
    }

    Transfer(UUID sourceAccountId, UUID targetAccountId, long amountMinor, String reference) {
        id = UUID.randomUUID();
        this.sourceAccountId = sourceAccountId;
        this.targetAccountId = targetAccountId;
        this.amountMinor = amountMinor;
        this.reference = reference;
        createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getSourceAccountId() { return sourceAccountId; }
    public UUID getTargetAccountId() { return targetAccountId; }
    public long getAmountMinor() { return amountMinor; }
    public String getReference() { return reference; }
    public Instant getCreatedAt() { return createdAt; }
}
