package com.bank.corebanking_prod.common.security;

import com.bank.corebanking_prod.feature.account.service.AccountService;
import com.bank.corebanking_prod.feature.auth.event.AuthorizationAuditEvent;
import com.bank.corebanking_prod.feature.auth.model.enums.UserRole;
import com.bank.corebanking_prod.feature.auth.security.AuthenticatedUser;
import com.bank.corebanking_prod.feature.customer.service.CustomerService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service("resourceOwnershipService")
public class ResourceOwnershipService {

    private final CustomerService customerService;
    private final AccountService accountService;
    private final ApplicationEventPublisher eventPublisher;

    public ResourceOwnershipService(
            CustomerService customerService,
            AccountService accountService,
            ApplicationEventPublisher eventPublisher) {
        this.customerService = customerService;
        this.accountService = accountService;
        this.eventPublisher = eventPublisher;
    }

    public boolean canAccessCustomer(Long requestedCustomerId) {
        AuthenticatedUser user = getAuthenticatedUser();
        if (user == null || requestedCustomerId == null) {
            return false;
        }
        if (isPrivilegedReadRole(user)) {
            publishPrivilegedRead(user, "READ_CUSTOMER", "CUSTOMER", requestedCustomerId);
            return true;
        }
        return user.getRole() == UserRole.CUSTOMER
                && requestedCustomerId.equals(user.getCustomerId());
    }

    public boolean canListAccountsForCustomer(Long requestedCustomerId) {
        AuthenticatedUser user = getAuthenticatedUser();
        if (user == null || requestedCustomerId == null) {
            return false;
        }
        if (isPrivilegedReadRole(user)) {
            publishPrivilegedRead(user, "LIST_CUSTOMER_ACCOUNTS", "CUSTOMER", requestedCustomerId);
            return true;
        }
        return user.getRole() == UserRole.CUSTOMER
                && requestedCustomerId.equals(user.getCustomerId());
    }

    public boolean canAccessCustomerByTckn(String tckn) {
        AuthenticatedUser user = getAuthenticatedUser();
        if (user == null) {
            return false;
        }
        if (isPrivilegedReadRole(user)) {
            publishPrivilegedRead(user, "READ_CUSTOMER_BY_TCKN", "CUSTOMER",
                    customerService.findCustomerIdByTckn(tckn));
            return true;
        }
        return user.getRole() == UserRole.CUSTOMER
                && customerService.customerMatchesTckn(user.getCustomerId(), tckn);
    }

    public boolean canAccessAccount(Long requestedAccountId) {
        AuthenticatedUser user = getAuthenticatedUser();
        if (user == null || requestedAccountId == null) {
            return false;
        }
        if (isPrivilegedReadRole(user)) {
            publishPrivilegedRead(user, "READ_ACCOUNT", "ACCOUNT", requestedAccountId);
            return true;
        }
        return user.getRole() == UserRole.CUSTOMER
                && accountService.accountBelongsToCustomer(requestedAccountId, user.getCustomerId());
    }

    public boolean canAccessAccountByIban(String iban) {
        AuthenticatedUser user = getAuthenticatedUser();
        if (user == null) {
            return false;
        }
        if (isPrivilegedReadRole(user)) {
            publishPrivilegedRead(user, "READ_ACCOUNT_BY_IBAN", "ACCOUNT", accountService.findAccountIdByIban(iban));
            return true;
        }
        return user.getRole() == UserRole.CUSTOMER
                && accountService.accountWithIbanBelongsToCustomer(iban, user.getCustomerId());
    }


    public boolean canListCustomers() {
        AuthenticatedUser user = getAuthenticatedUser();
        if (user == null) {
            return false;
        }
        if (isPrivilegedReadRole(user)) {
            publishPrivilegedRead(user, "LIST_CUSTOMERS", "CUSTOMER", null);
            return true;
        }
        return false;
    }

    public boolean canInitiateTransferFrom(String senderIban) {
        AuthenticatedUser user = getAuthenticatedUser();
        if (user == null) {
            return false;
        }
        if (user.getRole() == UserRole.BANK_OPERATOR || user.getRole() == UserRole.ADMIN) {
            return true;
        }
        return user.getRole() == UserRole.CUSTOMER
                && accountService.accountWithIbanBelongsToCustomer(senderIban, user.getCustomerId());
    }

    private boolean isPrivilegedReadRole(AuthenticatedUser user) {
        return user.getRole() == UserRole.BANK_OPERATOR
                || user.getRole() == UserRole.AUDITOR
                || user.getRole() == UserRole.ADMIN;
    }

    private void publishPrivilegedRead(
            AuthenticatedUser user,
            String action,
            String resourceType,
            Long resourceId) {
        eventPublisher.publishEvent(new AuthorizationAuditEvent(
                Instant.now(),
                user.getId(),
                AuthorizationAuditEvent.Decision.ALLOWED,
                action,
                resourceType,
                resourceId
        ));
    }

    private AuthenticatedUser getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user;
        }
        return null;
    }
}
