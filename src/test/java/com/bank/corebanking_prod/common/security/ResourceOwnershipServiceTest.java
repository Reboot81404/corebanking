package com.bank.corebanking_prod.common.security;

import com.bank.corebanking_prod.feature.account.service.AccountService;
import com.bank.corebanking_prod.feature.auth.event.AuthorizationAuditEvent;
import com.bank.corebanking_prod.feature.auth.model.enums.UserRole;
import com.bank.corebanking_prod.feature.auth.security.AuthenticatedUser;
import com.bank.corebanking_prod.feature.customer.service.CustomerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourceOwnershipServiceTest {

    @Mock
    private CustomerService customerService;
    @Mock
    private AccountService accountService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ResourceOwnershipService resourceOwnershipService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(UserRole role, Long customerId) {
        AuthenticatedUser user = new AuthenticatedUser(1L, "test_user", "pass", role, customerId, true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );
    }

    @Test
    @DisplayName("Customer users can access only their own customer profile")
    void shouldAllowCustomerOnlyToOwnId() {
        authenticateAs(UserRole.CUSTOMER, 10L);
        assertTrue(resourceOwnershipService.canAccessCustomer(10L));
        assertFalse(resourceOwnershipService.canAccessCustomer(20L));
    }

    @Test
    @DisplayName("Staff and auditor roles can read customer profiles")
    void shouldAllowStaffAndAuditorToReadCustomers() {
        authenticateAs(UserRole.BANK_OPERATOR, null);
        assertTrue(resourceOwnershipService.canAccessCustomer(10L));
        authenticateAs(UserRole.AUDITOR, null);
        assertTrue(resourceOwnershipService.canAccessCustomer(20L));
        verify(eventPublisher).publishEvent(argThat((Object event) ->
                event instanceof AuthorizationAuditEvent auditEvent
                        && auditEvent.actorIdentityId().equals(1L)
                        && auditEvent.action().equals("READ_CUSTOMER")
                        && auditEvent.resourceType().equals("CUSTOMER")
                        && auditEvent.resourceId().equals(20L)));
        authenticateAs(UserRole.ADMIN, null);
        assertTrue(resourceOwnershipService.canAccessCustomer(10L));
        verify(eventPublisher, times(3)).publishEvent(any(AuthorizationAuditEvent.class));
    }

    @Test
    @DisplayName("Customer account reads are checked through the account service")
    void shouldAllowCustomerOnlyToOwnAccount() {
        authenticateAs(UserRole.CUSTOMER, 10L);
        when(accountService.accountBelongsToCustomer(100L, 10L)).thenReturn(true);
        when(accountService.accountBelongsToCustomer(200L, 10L)).thenReturn(false);

        assertTrue(resourceOwnershipService.canAccessAccount(100L));
        assertFalse(resourceOwnershipService.canAccessAccount(200L));
    }

    @Test
    @DisplayName("Customer may transfer only from their own IBAN")
    void shouldAllowTransferOnlyFromOwnIban() {
        authenticateAs(UserRole.CUSTOMER, 10L);
        when(accountService.accountWithIbanBelongsToCustomer("TR1000", 10L)).thenReturn(true);
        when(accountService.accountWithIbanBelongsToCustomer("TR2000", 10L)).thenReturn(false);

        assertTrue(resourceOwnershipService.canInitiateTransferFrom("TR1000"));
        assertFalse(resourceOwnershipService.canInitiateTransferFrom("TR2000"));
    }

    @Test
    @DisplayName("Auditor TCKN and IBAN reads publish only stable resource IDs")
    void shouldAuditAuditorLookupsWithoutPublishingPersonalIdentifiers() {
        authenticateAs(UserRole.AUDITOR, null);
        when(customerService.findCustomerIdByTckn("10000000146")).thenReturn(10L);
        when(accountService.findAccountIdByIban("TR000000000000000000000001")).thenReturn(100L);

        assertTrue(resourceOwnershipService.canAccessCustomerByTckn("10000000146"));
        assertTrue(resourceOwnershipService.canAccessAccountByIban("TR000000000000000000000001"));

        verify(eventPublisher).publishEvent(argThat((Object event) ->
                event instanceof AuthorizationAuditEvent auditEvent
                        && auditEvent.action().equals("READ_CUSTOMER_BY_TCKN")
                        && auditEvent.resourceId().equals(10L)));
        verify(eventPublisher).publishEvent(argThat((Object event) ->
                event instanceof AuthorizationAuditEvent auditEvent
                        && auditEvent.action().equals("READ_ACCOUNT_BY_IBAN")
                        && auditEvent.resourceId().equals(100L)));
    }

    @Test
    @DisplayName("Auditor account listings by customer are audited separately")
    void shouldAuditAuditorCustomerAccountList() {
        authenticateAs(UserRole.AUDITOR, null);
        assertTrue(resourceOwnershipService.canListAccountsForCustomer(20L));
        verify(eventPublisher).publishEvent(argThat((Object event) ->
                event instanceof AuthorizationAuditEvent auditEvent
                        && auditEvent.action().equals("LIST_CUSTOMER_ACCOUNTS")
                        && auditEvent.resourceType().equals("CUSTOMER")
                        && auditEvent.resourceId().equals(20L)));

        authenticateAs(UserRole.CUSTOMER, 10L);
        assertTrue(resourceOwnershipService.canListAccountsForCustomer(10L));
        assertFalse(resourceOwnershipService.canListAccountsForCustomer(20L));
    }

    @Test
    @DisplayName("Auditor customer listings are audited")
    void shouldAuditAuditorCustomerList() {
        authenticateAs(UserRole.AUDITOR, null);
        assertTrue(resourceOwnershipService.canListCustomers());
        verify(eventPublisher).publishEvent(argThat((Object event) ->
                event instanceof AuthorizationAuditEvent auditEvent
                        && auditEvent.action().equals("LIST_CUSTOMERS")
                        && auditEvent.resourceType().equals("CUSTOMER")
                        && auditEvent.resourceId() == null));

        authenticateAs(UserRole.CUSTOMER, 10L);
        assertFalse(resourceOwnershipService.canListCustomers());
    }

    @Test
    @DisplayName("Auditors cannot initiate transfers")
    void auditorCannotInitiateTransfer() {
        authenticateAs(UserRole.AUDITOR, null);
        assertFalse(resourceOwnershipService.canInitiateTransferFrom("TR1000"));
        verifyNoInteractions(accountService);
    }
}
