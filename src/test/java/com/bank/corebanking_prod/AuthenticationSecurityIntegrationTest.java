package com.bank.corebanking_prod;

import com.bank.corebanking_prod.feature.account.model.entity.AccountEntity;
import com.bank.corebanking_prod.feature.account.model.enums.Currency;
import com.bank.corebanking_prod.feature.account.repository.AccountRepository;
import com.bank.corebanking_prod.feature.auth.model.dto.LoginRequest;
import com.bank.corebanking_prod.feature.auth.model.enums.UserRole;
import com.bank.corebanking_prod.feature.auth.repository.UserIdentityRepository;
import com.bank.corebanking_prod.feature.ledger.service.LedgerAccountService;
import com.bank.corebanking_prod.feature.payment.repository.PaymentIdempotencyRepository;
import com.bank.corebanking_prod.feature.payment.util.PayloadHashCalculator;
import com.bank.corebanking_prod.feature.auth.service.AuthenticationService;
import com.bank.corebanking_prod.feature.customer.model.entity.CustomerEntity;
import com.bank.corebanking_prod.feature.customer.repository.CustomerRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Exercises the real Spring Security filter chain and Flyway schema using an isolated H2 PostgreSQL-mode database. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:authdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=true",
        "spring.flyway.baseline-on-migrate=false",
        "spring.jpa.hibernate.ddl-auto=validate",
        "springdoc.api-docs.enabled=false",
        "springdoc.swagger-ui.enabled=false"
})
class AuthenticationSecurityIntegrationTest {

    private static final String PASSWORD = "TestPassword123!";
    private static final String JWT_SECRET = "test-only-signing-secret-with-at-least-256-bits-for-corebanking";
    private static final String JWT_ISSUER = "corebanking-prod";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private UserIdentityRepository userIdentityRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private LedgerAccountService ledgerAccountService;
    @Autowired
    private PaymentIdempotencyRepository paymentIdempotencyRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private Flyway flyway;

    private CustomerEntity customerA;
    private CustomerEntity customerB;
    private AccountEntity accountA;
    private AccountEntity accountB;

    @BeforeEach
    void setUp() {
        customerA = customerRepository.findByTckn("10000000146")
                .orElseGet(() -> customerRepository.save(new CustomerEntity(
                        "10000000146", "Customer", "A", "cust_a@bank.test", "05551234561"
                )));
        customerB = customerRepository.findByTckn("10000000147")
                .orElseGet(() -> customerRepository.save(new CustomerEntity(
                        "10000000147", "Customer", "B", "cust_b@bank.test", "05551234562"
                )));

        provisionIfMissing("customer_a", UserRole.CUSTOMER, customerA.getId());
        provisionIfMissing("customer_b", UserRole.CUSTOMER, customerB.getId());
        provisionIfMissing("bank_operator", UserRole.BANK_OPERATOR, null);
        provisionIfMissing("auditor_user", UserRole.AUDITOR, null);
        provisionIfMissing("admin_user", UserRole.ADMIN, null);
        provisionIfMissing("disabled_user", UserRole.ADMIN, null);
        provisionIfMissing("locked_user", UserRole.ADMIN, null);
        for (String username : new String[]{
                "customer_a", "customer_b", "bank_operator", "auditor_user",
                "admin_user", "disabled_user", "locked_user"
        }) {
            userIdentityRepository.findByUsername(username).ifPresent(identity -> {
                identity.setStatus(com.bank.corebanking_prod.feature.auth.model.enums.UserStatus.ACTIVE);
                userIdentityRepository.saveAndFlush(identity);
            });
        }

        accountA = accountRepository.findByIban("TR000000000000000000000001")
                .orElseGet(() -> saveAccount("TR000000000000000000000001", customerA));
        accountB = accountRepository.findByIban("TR000000000000000000000002")
                .orElseGet(() -> saveAccount("TR000000000000000000000002", customerB));
        ledgerAccountService.getOrCreateCustomerDepositAccount(accountA.getId(), accountA.getIban(), accountA.getCurrency());
        ledgerAccountService.getOrCreateCustomerDepositAccount(accountB.getId(), accountB.getIban(), accountB.getCurrency());
    }

    @Test
    @DisplayName("Flyway migrations create authentication and principal-scoped idempotency schemas")
    void shouldApplyAuthenticationMigration() {
        assertNotNull(flyway.info().current());
        assertEquals("5", flyway.info().current().getVersion().getVersion());
    }

    @Test
    @DisplayName("Valid login issues bearer token; unknown, incorrect, and disabled credentials all return 401")
    void shouldAuthenticateProvisionedUsersAndRejectInvalidCredentials() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest("customer_a", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.customerId").doesNotExist());

        assertUnauthorizedLogin("customer_a", "WrongPassword123!");
        assertUnauthorizedLogin("unregistered_user", PASSWORD);

        userIdentityRepository.findByUsername("disabled_user").ifPresent(identity -> {
            identity.setStatus(com.bank.corebanking_prod.feature.auth.model.enums.UserStatus.DISABLED);
            userIdentityRepository.saveAndFlush(identity);
        });
        assertUnauthorizedLogin("disabled_user", PASSWORD);

        userIdentityRepository.findByUsername("locked_user").ifPresent(identity -> {
            identity.setStatus(com.bank.corebanking_prod.feature.auth.model.enums.UserStatus.LOCKED);
            userIdentityRepository.saveAndFlush(identity);
        });
        assertUnauthorizedLogin("locked_user", PASSWORD);
    }

    @Test
    @DisplayName("Changing identity status invalidates already-issued tokens on the next request")
    void shouldRecheckIdentityStatusForEveryBearerRequest() throws Exception {
        String token = loginAndGetToken("customer_a", PASSWORD);
        userIdentityRepository.findByUsername("customer_a").ifPresent(identity -> {
            identity.setStatus(com.bank.corebanking_prod.feature.auth.model.enums.UserStatus.DISABLED);
            userIdentityRepository.saveAndFlush(identity);
        });

        mockMvc.perform(get("/api/v1/customers/{id}", customerA.getId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Missing, malformed, expired, tampered, and wrong-issuer tokens return HTTP 401")
    void shouldRejectInvalidBearerTokens() throws Exception {
        mockMvc.perform(get("/api/v1/customers/{id}", customerA.getId()))
                .andExpect(status().isUnauthorized());

        assertProtectedRequestUnauthorized("not.a.jwt");
        assertProtectedRequestUnauthorized(expiredToken());
        assertProtectedRequestUnauthorized(wrongIssuerToken());

        String validToken = loginAndGetToken("customer_a", PASSWORD);
        assertProtectedRequestUnauthorized(validToken + "tampered");
    }

    @Test
    @DisplayName("Customer access is restricted to own customer, account, and sender IBAN")
    void shouldEnforceCustomerOwnershipAcrossResources() throws Exception {
        String customerToken = loginAndGetToken("customer_a", PASSWORD);

        mockMvc.perform(get("/api/v1/customers/{id}", customerA.getId())
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customerA.getId()));

        mockMvc.perform(get("/api/v1/customers/{id}", customerB.getId())
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/customers/tckn/{tckn}", customerA.getTckn())
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/customers/tckn/{tckn}", customerB.getTckn())
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/accounts/{id}", accountA.getId())
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/accounts/iban/{iban}", accountA.getIban())
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/accounts/{id}", accountB.getId())
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/accounts/iban/{iban}", accountB.getIban())
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/accounts/customer/{customerId}", customerB.getId())
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/accounts/customer/{customerId}", customerA.getId())
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/payments/transfer")
                        .header("Authorization", bearer(customerToken))
                        .header("Idempotency-Key", "ownership-transfer-test-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"senderIban":"%s","receiverIban":"%s","amount":10.00}
                                """.formatted(accountB.getIban(), accountA.getIban())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Role matrix differentiates customer, operator, auditor, and admin")
    void shouldEnforceRoleMatrix() throws Exception {
        String customerToken = loginAndGetToken("customer_a", PASSWORD);
        String operatorToken = loginAndGetToken("bank_operator", PASSWORD);
        String auditorToken = loginAndGetToken("auditor_user", PASSWORD);
        String adminToken = loginAndGetToken("admin_user", PASSWORD);

        mockMvc.perform(get("/api/v1/customers/{id}", customerB.getId())
                        .header("Authorization", bearer(auditorToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/customers")
                        .header("Authorization", bearer(auditorToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/accounts/{id}", accountB.getId())
                        .header("Authorization", bearer(auditorToken)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newCustomerJson("10000000214", "customer-created-denied")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", bearer(auditorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newCustomerJson("10000000214", "auditor-created-denied")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", bearer(operatorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newCustomerJson("10000000214", "operator-created")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newCustomerJson("10000000078", "admin-created")))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/api/v1/customers/{id}", customerA.getId())
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Changed","lastName":"Name","email":"changed@bank.test","phoneNumber":"05551234563"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/customers")
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Payment idempotency keys are scoped to the authenticated identity")
    void shouldScopePaymentIdempotencyKeysByAuthenticatedIdentity() throws Exception {
        String operatorToken = loginAndGetToken("bank_operator", PASSWORD);
        String adminToken = loginAndGetToken("admin_user", PASSWORD);
        String key = "principal-scope-key-001";
        String transferBody = """
                {"senderIban":"%s","receiverIban":"%s","amount":1.00}
                """.formatted(accountA.getIban(), accountB.getIban());

        mockMvc.perform(post("/api/v1/payments/transfer")
                        .header("Authorization", bearer(operatorToken))
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferBody))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/payments/transfer")
                        .header("Authorization", bearer(adminToken))
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferBody))
                .andExpect(status().isOk());

        Long operatorId = userIdentityRepository.findByUsername("bank_operator").orElseThrow().getId();
        Long adminId = userIdentityRepository.findByUsername("admin_user").orElseThrow().getId();
        assertTrue(paymentIdempotencyRepository.findByPrincipalIdAndOperationAndIdempotencyKey(
                operatorId, PayloadHashCalculator.OPERATION_TRANSFER_V1, key).isPresent());
        assertTrue(paymentIdempotencyRepository.findByPrincipalIdAndOperationAndIdempotencyKey(
                adminId, PayloadHashCalculator.OPERATION_TRANSFER_V1, key).isPresent());
    }

    @Test
    @DisplayName("Only ADMIN can provision identities and response never includes the password hash")
    void shouldRestrictIdentityProvisioningToAdmin() throws Exception {
        String operatorToken = loginAndGetToken("bank_operator", PASSWORD);
        String adminToken = loginAndGetToken("admin_user", PASSWORD);
        String provisionRequest = """
                {"username":"new_customer","password":"NewCustomerPass123!","role":"CUSTOMER","customerId":%d}
                """.formatted(customerB.getId());

        mockMvc.perform(post("/api/auth/users")
                        .header("Authorization", bearer(operatorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(provisionRequest))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/auth/users")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(provisionRequest))
                .andExpect(status().isConflict()); // customer B already has its one identity

        String validProvisionRequest = """
                {"username":"new_customer_2","password":"NewCustomerPass123!","role":"CUSTOMER","customerId":%d}
                """.formatted(customerA.getId() + 10_000L);
        // The non-existent customer link is rejected without creating an identity.
        mockMvc.perform(post("/api/auth/users")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProvisionRequest))
                .andExpect(status().isConflict());

        String staffProvisionRequest = """
                {"username":"new_staff_admin","password":"NewStaffPassword123!","role":"BANK_OPERATOR"}
                """;
        MvcResult result = mockMvc.perform(post("/api/auth/users")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(staffProvisionRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("new_staff_admin"))
                .andExpect(jsonPath("$.role").value("BANK_OPERATOR"))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        assertFalse(response.has("password"));
        assertFalse(response.has("passwordHash"));
    }

    private void provisionIfMissing(String username, UserRole role, Long customerId) {
        if (!userIdentityRepository.existsByUsername(username)) {
            authenticationService.provisionUser(username, PASSWORD, role, customerId);
        }
    }

    private AccountEntity saveAccount(String iban, CustomerEntity customer) {
        AccountEntity account = new AccountEntity();
        account.setIban(iban);
        account.setBalance(new BigDecimal("100.00"));
        account.setCurrency(Currency.TRY);
        account.setCustomer(customer);
        return accountRepository.save(account);
    }

    private void assertUnauthorizedLogin(String username, String password) throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest(username, password))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password."));
    }

    private void assertProtectedRequestUnauthorized(String token) throws Exception {
        mockMvc.perform(get("/api/v1/customers/{id}", customerA.getId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest(username, password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private String expiredToken() {
        Date now = new Date();
        return Jwts.builder()
                .issuer(JWT_ISSUER)
                .subject("customer_a")
                .claim("role", UserRole.CUSTOMER.name())
                .issuedAt(new Date(now.getTime() - 10_000))
                .expiration(new Date(now.getTime() - 1_000))
                .signWith(signingKey())
                .compact();
    }

    private String wrongIssuerToken() {
        Date now = new Date();
        return Jwts.builder()
                .issuer("untrusted-issuer")
                .subject("customer_a")
                .claim("role", UserRole.CUSTOMER.name())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 60_000))
                .signWith(signingKey())
                .compact();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String newCustomerJson(String tckn, String emailLocalPart) {
        return """
                {"tckn":"%s","firstName":"New","lastName":"Customer","email":"%s@bank.test","phoneNumber":"05551234568"}
                """.formatted(tckn, emailLocalPart);
    }
}
