package com.bank.corebanking_prod.feature.auth.bootstrap;

import com.bank.corebanking_prod.feature.auth.model.enums.UserRole;
import com.bank.corebanking_prod.feature.auth.repository.UserIdentityRepository;
import com.bank.corebanking_prod.feature.auth.service.AuthenticationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * One-time bootstrap path for the first administrator. It is disabled by default
 * and skips provisioning as soon as an ADMIN identity already exists.
 */
@Component
@ConditionalOnProperty(prefix = "security.bootstrap-admin", name = "enabled", havingValue = "true")
public class InitialAdminBootstrapRunner implements ApplicationRunner {

    private final UserIdentityRepository userIdentityRepository;
    private final AuthenticationService authenticationService;
    private String username;
    private String password;

    public InitialAdminBootstrapRunner(
            UserIdentityRepository userIdentityRepository,
            AuthenticationService authenticationService,
            @Value("${security.bootstrap-admin.username:}") String username,
            @Value("${security.bootstrap-admin.password:}") String password) {
        this.userIdentityRepository = userIdentityRepository;
        this.authenticationService = authenticationService;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userIdentityRepository.existsByRole(UserRole.ADMIN)) {
            return;
        }
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new IllegalStateException(
                    "ADMIN_BOOTSTRAP_USERNAME and ADMIN_BOOTSTRAP_PASSWORD are required when admin bootstrap is enabled."
            );
        }

        String bootstrapUsername = username;
        String bootstrapPassword = password;
        username = null;
        password = null;
        authenticationService.provisionUser(bootstrapUsername, bootstrapPassword, UserRole.ADMIN, null);
    }
}
