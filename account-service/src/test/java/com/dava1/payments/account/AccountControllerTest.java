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

    @Test
    void opensAndReadsAccount() throws Exception {
        String created = mvc.perform(post("/api/v1/accounts")
                        .contentType("application/json")
                        .content("{\"ownerId\":\"customer-1\",\"currency\":\"EUR\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.balanceMinor").value(0))
                .andReturn().getResponse().getContentAsString();

        UUID id = UUID.fromString(new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(created).get("id").asText());

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

        String self = transferRequest(source.getId(), source.getId(), 100, "self");
        mvc.perform(post("/api/v1/transfers").contentType("application/json").content(self))
                .andExpect(status().isBadRequest());

        String insufficient = transferRequest(source.getId(), target.getId(), 100, "insufficient");
        mvc.perform(post("/api/v1/transfers").contentType("application/json").content(insufficient))
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

    private String transferRequest(UUID source, UUID target, long amount, String ref) {
        return """
                {"sourceAccountId":"%s","targetAccountId":"%s","amountMinor":%d,"reference":"%s"}
                """.formatted(source, target, amount, ref);
    }
}
