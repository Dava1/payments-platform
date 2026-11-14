package com.dava1.payments.account;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AccountControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    AccountRepository accounts;

    @Autowired
    LedgerEntryRepository entries;

    @Test
    void opensAndReadsAccount() throws Exception {
        var response = mvc.perform(post("/api/v1/accounts")
                        .contentType("application/json")
                        .content("{\"ownerId\":\"customer-1\",\"currency\":\"EUR\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.balanceMinor").value(0))
                .andReturn().getResponse();

        String location = response.getHeader("Location");
        org.junit.jupiter.api.Assertions.assertNotNull(location);
        UUID id = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));

        mvc.perform(get("/api/v1/accounts/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerId").value("customer-1"));

        mvc.perform(get("/api/v1/accounts/{id}/entries", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void rejectsInvalidAccountInput() throws Exception {
        mvc.perform(post("/api/v1/accounts")
                        .contentType("application/json")
                        .content("{\"ownerId\":\"\",\"currency\":\"EURO\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns404ForMissingAccountAndTransfer() throws Exception {
        mvc.perform(get("/api/v1/accounts/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/transfers/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsSelfTransferAndInsufficientFunds() throws Exception {
        Account source = accounts.saveAndFlush(new Account("source", "EUR"));
        Account target = accounts.saveAndFlush(new Account("target", "EUR"));

        mvc.perform(post("/api/v1/transfers")
                        .contentType("application/json")
                        .content(transferRequest(source.getId(), source.getId(), 100, "self")))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/v1/transfers")
                        .contentType("application/json")
                        .content(transferRequest(source.getId(), target.getId(), 100, "insufficient")))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void rejectsCrossCurrencyTransfers() throws Exception {
        Account source = accounts.saveAndFlush(new Account("source", "EUR"));
        Account target = accounts.saveAndFlush(new Account("target", "USD"));

        mvc.perform(post("/api/v1/transfers")
                        .contentType("application/json")
                        .content(transferRequest(source.getId(), target.getId(), 100, "currency")))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void transfersOnceForRepeatedReferenceAndCreatesBalancedEntries() throws Exception {
        Account source = new Account("source", "EUR");
        Account target = new Account("target", "EUR");
        // Seed a balance as a test fixture; funding is part of the separate ledger domain issue.
        source.adjustBalance(1_000);
        accounts.saveAndFlush(source);
        accounts.saveAndFlush(target);

        String request = transferRequest(source.getId(), target.getId(), 350, "order-123");
        String firstLocation = mvc.perform(post("/api/v1/transfers")
                        .contentType("application/json").content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amountMinor").value(350))
                .andReturn().getResponse().getHeader("Location");

        String repeatedLocation = mvc.perform(post("/api/v1/transfers")
                        .contentType("application/json").content(request))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");

        org.junit.jupiter.api.Assertions.assertEquals(firstLocation, repeatedLocation);
        mvc.perform(get("/api/v1/accounts/{id}", source.getId()))
                .andExpect(jsonPath("$.balanceMinor").value(650));
        mvc.perform(get("/api/v1/accounts/{id}", target.getId()))
                .andExpect(jsonPath("$.balanceMinor").value(350));

        var sourceEntries = entries.findByAccountId(source.getId(),
                org.springframework.data.domain.PageRequest.of(0, 10)).getContent();
        var targetEntries = entries.findByAccountId(target.getId(),
                org.springframework.data.domain.PageRequest.of(0, 10)).getContent();

        org.junit.jupiter.api.Assertions.assertEquals(1, sourceEntries.size());
        org.junit.jupiter.api.Assertions.assertEquals(1, targetEntries.size());
        org.junit.jupiter.api.Assertions.assertEquals(LedgerEntry.Direction.DEBIT,
                sourceEntries.getFirst().getDirection());
        org.junit.jupiter.api.Assertions.assertEquals(LedgerEntry.Direction.CREDIT,
                targetEntries.getFirst().getDirection());

        mvc.perform(post("/api/v1/transfers").contentType("application/json")
                        .content(transferRequest(source.getId(), target.getId(), 400, "order-123")))
                .andExpect(status().isConflict());
    }

    private String transferRequest(UUID source, UUID target, long amount, String ref) {
        return """
                {"sourceAccountId":"%s","targetAccountId":"%s","amountMinor":%d,"reference":"%s"}
                """.formatted(source, target, amount, ref);
    }
}
