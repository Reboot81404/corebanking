package com.bank.corebanking_prod.feature.auth.service;

import com.bank.corebanking_prod.common.exception.BusinessException;
import com.bank.corebanking_prod.feature.auth.event.AuthenticationAuditEvent;
import com.bank.corebanking_prod.feature.auth.model.dto.LoginRequest;
import com.bank.corebanking_prod.feature.auth.model.dto.TokenResponse;
import com.bank.corebanking_prod.feature.auth.model.entity.UserIdentityEntity;
import com.bank.corebanking_prod.feature.auth.model.enums.UserRole;
import com.bank.corebanking_prod.feature.auth.model.enums.UserStatus;
import com.bank.corebanking_prod.feature.auth.repository.UserIdentityRepository;
import com.bank.corebanking_prod.feature.auth.security.AuthenticatedUser;
import com.bank.corebanking_prod.feature.customer.service.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserIdentityRepository userIdentityRepository;
    @Mock
    private CustomerService customerService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtTokenService jwtTokenService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    private UserIdentityEntity activeUser;

    @BeforeEach
    void setUp() {
        activeUser = new UserIdentityEntity(
                "john_doe",
                "$2a$10$hashedpassword",
                UserRole.CUSTOMER,
                UserStatus.ACTIVE,
                42L
        );
        activeUser.setId(7L);
    }

    @Test
    @DisplayName("Valid credentials authenticate and return a short-lived bearer token")
    void shouldLoginSuccessfullyWithValidCredentials() {
        AuthenticatedUser principal = principal(activeUser);
        when(authenticationManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
        when(jwtTokenService.generateToken("john_doe", UserRole.CUSTOMER)).thenReturn("valid.jwt.token");
        when(jwtTokenService.getExpirationMs()).thenReturn(900_000L);

        TokenResponse response = authenticationService.login(new LoginRequest(" JOHN_DOE ", "Password123!"));

        assertEquals("valid.jwt.token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(900L, response.expiresIn());
        assertEquals(UserRole.CUSTOMER, response.role());
        verify(authenticationManager).authenticate(argThat(authentication ->
                "john_doe".equals(authentication.getPrincipal())
                        && "Password123!".equals(authentication.getCredentials())));
        verify(eventPublisher).publishEvent(argThat((Object event) ->
                event instanceof AuthenticationAuditEvent auditEvent
                        && auditEvent.outcome() == AuthenticationAuditEvent.Outcome.SUCCESS
                        && Long.valueOf(7L).equals(auditEvent.actorIdentityId())));
    }

    @Test
    @DisplayName("Unknown usernames and incorrect passwords have the same authentication response")
    void shouldReturnGenericAuthenticationFailure() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("user not found"));

        BadCredentialsException exception = assertThrows(BadCredentialsException.class,
                () -> authenticationService.login(new LoginRequest("unknown_user", "Password123!")));

        assertEquals("Invalid username or password.", exception.getMessage());
        verify(eventPublisher).publishEvent(argThat((Object event) ->
                event instanceof AuthenticationAuditEvent auditEvent
                        && auditEvent.outcome() == AuthenticationAuditEvent.Outcome.FAILURE
                        && auditEvent.actorIdentityId() == null));
    }

    @Test
    @DisplayName("Locked and disabled users receive the same generic authentication failure")
    void shouldNotRevealLockedOrDisabledStatus() {
        when(authenticationManager.authenticate(any())).thenThrow(new DisabledException("disabled"));

        BadCredentialsException exception = assertThrows(BadCredentialsException.class,
                () -> authenticationService.login(new LoginRequest("john_doe", "Password123!")));

        assertEquals("Invalid username or password.", exception.getMessage());
    }

    @Test
    @DisplayName("Locked identities receive the same generic authentication failure")
    void shouldNotRevealLockedAccountStatus() {
        when(authenticationManager.authenticate(any())).thenThrow(new LockedException("locked"));

        BadCredentialsException exception = assertThrows(BadCredentialsException.class,
                () -> authenticationService.login(new LoginRequest("john_doe", "Password123!")));

        assertEquals("Invalid username or password.", exception.getMessage());
    }

    @Test
    @DisplayName("Provisioning stores only a BCrypt password hash and normalizes username")
    void shouldProvisionUserWithHashedPassword() {
        when(userIdentityRepository.existsByUsername("new_staff")).thenReturn(false);
        when(passwordEncoder.encode("StrongPassword123!")).thenReturn("$2a$10$hashedStrongPassword");
        when(userIdentityRepository.save(any(UserIdentityEntity.class))).thenAnswer(invocation -> {
            UserIdentityEntity entity = invocation.getArgument(0);
            entity.setId(11L);
            return entity;
        });

        UserIdentityEntity provisioned = authenticationService.provisionUser(
                " NEW_STAFF ", "StrongPassword123!", UserRole.BANK_OPERATOR, null
        );

        assertEquals("new_staff", provisioned.getUsername());
        assertEquals("$2a$10$hashedStrongPassword", provisioned.getPasswordHash());
        assertEquals(UserRole.BANK_OPERATOR, provisioned.getRole());
        assertEquals(UserStatus.ACTIVE, provisioned.getStatus());
        assertNull(provisioned.getCustomerId());
    }

    @Test
    @DisplayName("Customer identities require an existing customer and can be assigned only once")
    void shouldRequireValidCustomerLinkForCustomerIdentity() {
        when(userIdentityRepository.existsByUsername("customer_one")).thenReturn(false);
        when(customerService.customerExists(42L)).thenReturn(true);
        when(userIdentityRepository.existsByCustomerId(42L)).thenReturn(true);

        assertThrows(BusinessException.class, () -> authenticationService.provisionUser(
                "customer_one", "StrongPassword123!", UserRole.CUSTOMER, 42L
        ));
    }

    @Test
    @DisplayName("Customer identity cannot be provisioned without a customer link")
    void shouldRejectCustomerIdentityWithoutCustomerId() {
        when(userIdentityRepository.existsByUsername("customer_one")).thenReturn(false);
        assertThrows(BusinessException.class, () -> authenticationService.provisionUser(
                "customer_one", "StrongPassword123!", UserRole.CUSTOMER, null
        ));
    }

    @Test
    @DisplayName("Staff identities cannot be linked to customer records")
    void shouldRejectStaffIdentityWithCustomerId() {
        when(userIdentityRepository.existsByUsername("staff_one")).thenReturn(false);
        assertThrows(BusinessException.class, () -> authenticationService.provisionUser(
                "staff_one", "StrongPassword123!", UserRole.ADMIN, 42L
        ));
    }

    @Test
    @DisplayName("Weak passwords are rejected before BCrypt encoding")
    void shouldRejectWeakProvisioningPassword() {
        when(userIdentityRepository.existsByUsername("staff_one")).thenReturn(false);
        assertThrows(BusinessException.class, () -> authenticationService.provisionUser(
                "staff_one", "short", UserRole.ADMIN, null
        ));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("Existing usernames cannot be provisioned again")
    void shouldRejectDuplicateUsername() {
        when(userIdentityRepository.existsByUsername("existing_user")).thenReturn(true);
        assertThrows(BusinessException.class, () -> authenticationService.provisionUser(
                "existing_user", "StrongPassword123!", UserRole.ADMIN, null
        ));
    }

    private AuthenticatedUser principal(UserIdentityEntity identity) {
        return new AuthenticatedUser(
                identity.getId(), identity.getUsername(), identity.getPasswordHash(),
                identity.getRole(), identity.getCustomerId(), true
        );
    }
}
