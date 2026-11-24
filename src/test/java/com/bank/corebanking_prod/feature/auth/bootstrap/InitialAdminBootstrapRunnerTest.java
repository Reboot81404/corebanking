package com.bank.corebanking_prod.feature.auth.bootstrap;

import com.bank.corebanking_prod.feature.auth.model.enums.UserRole;
import com.bank.corebanking_prod.feature.auth.repository.UserIdentityRepository;
import com.bank.corebanking_prod.feature.auth.service.AuthenticationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InitialAdminBootstrapRunnerTest {

    @Mock
    private UserIdentityRepository userIdentityRepository;
    @Mock
    private AuthenticationService authenticationService;

    @Test
    void shouldSkipBootstrapWhenAnAdminAlreadyExists() {
        when(userIdentityRepository.existsByRole(UserRole.ADMIN)).thenReturn(true);
        InitialAdminBootstrapRunner runner = new InitialAdminBootstrapRunner(
                userIdentityRepository, authenticationService, "initial_admin", "StrongPassword123!"
        );

        runner.run(null);

        verify(userIdentityRepository).existsByRole(UserRole.ADMIN);
        verifyNoInteractions(authenticationService);
    }

    @Test
    void shouldProvisionFirstAdminWhenExplicitlyConfigured() {
        when(userIdentityRepository.existsByRole(UserRole.ADMIN)).thenReturn(false);
        InitialAdminBootstrapRunner runner = new InitialAdminBootstrapRunner(
                userIdentityRepository, authenticationService, "initial_admin", "StrongPassword123!"
        );

        runner.run(null);

        verify(authenticationService).provisionUser(
                "initial_admin", "StrongPassword123!", UserRole.ADMIN, null
        );
    }

    @Test
    void shouldFailClosedWhenBootstrapCredentialsAreMissing() {
        when(userIdentityRepository.existsByRole(UserRole.ADMIN)).thenReturn(false);
        InitialAdminBootstrapRunner runner = new InitialAdminBootstrapRunner(
                userIdentityRepository, authenticationService, "", ""
        );

        assertThrows(IllegalStateException.class, () -> runner.run(null));
        verifyNoInteractions(authenticationService);
    }
}
