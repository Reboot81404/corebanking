package com.bank.corebanking_prod.feature.account.controller;

import com.bank.corebanking_prod.feature.account.model.dto.AccountDto;
import com.bank.corebanking_prod.feature.account.model.enums.Currency;
import com.bank.corebanking_prod.feature.account.service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@AutoConfigureMockMvc(addFilters = false)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AccountService accountService;

    @Test
    @DisplayName("Başlangıç bakiyesi verilmediğinde (null) 201 Created dönmeli ve bakiye 0.00 olmalı")
    void shouldCreateAccountWithZeroBalanceWhenBalanceOmitted() throws Exception {
        AccountDto.Response response = new AccountDto.Response(
                1L,
                "TR123456789012345678901234",
                BigDecimal.ZERO,
                Currency.TRY,
                10L,
                LocalDateTime.now()
        );
        when(accountService.createAccount(any(AccountDto.Create.class))).thenReturn(response);

        String json = """
                {
                    "customerId": 10,
                    "currency": "TRY"
                }
                """;

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.balance").value(0))
                .andExpect(jsonPath("$.currency").value("TRY"));

        verify(accountService).createAccount(any(AccountDto.Create.class));
    }

    @Test
    @DisplayName("Başlangıç bakiyesi 0.00 verildiğinde 201 Created dönmeli ve bakiye 0.00 olmalı")
    void shouldCreateAccountWithZeroBalanceWhenZeroBalanceProvided() throws Exception {
        AccountDto.Response response = new AccountDto.Response(
                1L,
                "TR123456789012345678901234",
                BigDecimal.ZERO,
                Currency.TRY,
                10L,
                LocalDateTime.now()
        );
        when(accountService.createAccount(any(AccountDto.Create.class))).thenReturn(response);

        String json = """
                {
                    "customerId": 10,
                    "currency": "TRY",
                    "balance": 0.00
                }
                """;

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.balance").value(0));

        verify(accountService).createAccount(any(AccountDto.Create.class));
    }

    @Test
    @DisplayName("Başlangıç bakiyesi pozitif verildiğinde HTTP 400 Bad Request dönmeli ve hesap oluşturulmamalı")
    void shouldReturnBadRequestWhenBalanceIsPositive() throws Exception {
        String json = """
                {
                    "customerId": 10,
                    "currency": "TRY",
                    "balance": 100.00
                }
                """;

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(accountService, never()).createAccount(any(AccountDto.Create.class));
    }

    @Test
    @DisplayName("Başlangıç bakiyesi negatif verildiğinde HTTP 400 Bad Request dönmeli ve hesap oluşturulmamalı")
    void shouldReturnBadRequestWhenBalanceIsNegative() throws Exception {
        String json = """
                {
                    "customerId": 10,
                    "currency": "TRY",
                    "balance": -50.00
                }
                """;

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(accountService, never()).createAccount(any(AccountDto.Create.class));
    }
}
