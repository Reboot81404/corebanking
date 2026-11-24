package com.bank.corebanking_prod.feature.payment.controller;

import com.bank.corebanking_prod.common.exception.IdempotencyConflictException;
import com.bank.corebanking_prod.feature.auth.model.enums.UserRole;
import com.bank.corebanking_prod.feature.auth.security.AuthenticatedUser;
import com.bank.corebanking_prod.feature.payment.model.dto.TransactionHistoryDto;
import com.bank.corebanking_prod.feature.payment.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockitoBean
    private PaymentService paymentService;

    private static final String SENDER_IBAN = "TR100000000000000000000001";
    private static final String RECEIVER_IBAN = "TR200000000000000000000002";
    private static final String VALID_KEY = "test-idempotency-key-12345";

    @Test
    @DisplayName("Idempotency-Key eksik olduğunda HTTP 409 dönmeli (BusinessException)")
    void shouldReturnBadRequestWhenIdempotencyKeyMissing() throws Exception {
        TransactionHistoryDto.TransferRequest request = new TransactionHistoryDto.TransferRequest(
                SENDER_IBAN,
                RECEIVER_IBAN,
                new BigDecimal("100.00")
        );

        mockMvc.perform(post("/api/v1/payments/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Başarılı transfer HTTP 200 dönmeli")
    void shouldReturnOkWhenTransferSuccessful() throws Exception {
        TransactionHistoryDto.TransferRequest request = new TransactionHistoryDto.TransferRequest(
                SENDER_IBAN,
                RECEIVER_IBAN,
                new BigDecimal("100.00")
        );

        TransactionHistoryDto.TransferResponse response = new TransactionHistoryDto.TransferResponse(
                "Transfer is completed.",
                SENDER_IBAN,
                RECEIVER_IBAN,
                new BigDecimal("100.00"),
                LocalDateTime.now()
        );

        when(paymentService.transferMoney(any(), eq(VALID_KEY), eq(42L))).thenReturn(response);

        mockMvc.perform(post("/api/v1/payments/transfer")
                        .header("Idempotency-Key", VALID_KEY)
                        .principal(testAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Transfer is completed."))
                .andExpect(jsonPath("$.amount").value(100.00));

        verify(paymentService).transferMoney(any(), eq(VALID_KEY), eq(42L));
    }

    private UsernamePasswordAuthenticationToken testAuthentication() {
        AuthenticatedUser user = new AuthenticatedUser(42L, "payment_operator", "unused", UserRole.BANK_OPERATOR, null, true);
        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }

    @Test
    @DisplayName("Aynı key farklı payload ile çağrıldığında HTTP 409 Conflict dönmeli")
    void shouldReturnConflictWhenPayloadDiffers() throws Exception {
        TransactionHistoryDto.TransferRequest request = new TransactionHistoryDto.TransferRequest(
                SENDER_IBAN,
                RECEIVER_IBAN,
                new BigDecimal("100.00")
        );

        when(paymentService.transferMoney(any(), eq(VALID_KEY), eq(42L)))
                .thenThrow(new IdempotencyConflictException("An existing transfer exists with the same Idempotency-Key but different request parameters."));

        mockMvc.perform(post("/api/v1/payments/transfer")
                        .header("Idempotency-Key", VALID_KEY)
                        .principal(testAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }
}
