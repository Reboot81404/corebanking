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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("[A-Za-z0-9._-]{3,50}");
    private static final int MIN_PASSWORD_LENGTH = 12;
    private static final int MAX_BCRYPT_PASSWORD_BYTES = 72;

    private final UserIdentityRepository userIdentityRepository;
    private final CustomerService customerService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    public AuthenticationServiceImpl(
            UserIdentityRepository userIdentityRepository,
            CustomerService customerService,
            AuthenticationManager authenticationManager,
            JwtTokenService jwtTokenService,
            PasswordEncoder passwordEncoder,
            ApplicationEventPublisher eventPublisher) {
        this.userIdentityRepository = userIdentityRepository;
        this.customerService = customerService;
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        if (request == null || request.getUsername() == null || request.getPassword() == null
                || request.getPassword().getBytes(StandardCharsets.UTF_8).length > MAX_BCRYPT_PASSWORD_BYTES) {
            publishAuthenticationFailure();
            throw invalidCredentials();
        }

        String normalizedUsername;
        try {
            normalizedUsername = normalizeUsername(request.getUsername());
        } catch (BusinessException exception) {
            publishAuthenticationFailure();
            throw invalidCredentials();
        }
        try {
            var authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedUsername, request.getPassword())
            );

            if (!(authentication.getPrincipal() instanceof AuthenticatedUser user) || !user.isEnabled()) {
                throw invalidCredentials();
            }

            String token = jwtTokenService.generateToken(user.getUsername(), user.getRole());
            eventPublisher.publishEvent(new AuthenticationAuditEvent(
                    Instant.now(),
                    AuthenticationAuditEvent.Outcome.SUCCESS,
                    user.getId(),
                    null,
                    "AUTHENTICATION_SUCCEEDED"
            ));

            return new TokenResponse(token, jwtTokenService.getExpirationMs() / 1000, user.getRole());
        } catch (AuthenticationException exception) {
            publishAuthenticationFailure();
            throw invalidCredentials();
        }
    }

    @Override
    @Transactional
    public UserIdentityEntity provisionUser(String username, String rawPassword, UserRole role, Long customerId) {
        String normalizedUsername = normalizeUsername(username);
        validateProvisioningPassword(rawPassword);

        if (role == null) {
            throw new BusinessException("A role must be assigned to the identity.");
        }
        if (userIdentityRepository.existsByUsername(normalizedUsername)) {
            throw new BusinessException("An identity with this username already exists.");
        }

        if (role == UserRole.CUSTOMER) {
            if (customerId == null || !customerService.customerExists(customerId)) {
                throw new BusinessException("A valid customer record must be linked to a CUSTOMER identity.");
            }
            if (userIdentityRepository.existsByCustomerId(customerId)) {
                throw new BusinessException("The customer already has an authentication identity.");
            }
        } else if (customerId != null) {
            throw new BusinessException("Staff identities must not be linked to a customer record.");
        }

        UserIdentityEntity identity = new UserIdentityEntity(
                normalizedUsername,
                // The database stores only the BCrypt hash. The raw password is never returned or logged.
                passwordEncoder.encode(rawPassword),
                role,
                UserStatus.ACTIVE,
                customerId
        );

        UserIdentityEntity saved = userIdentityRepository.save(identity);
        eventPublisher.publishEvent(new AuthenticationAuditEvent(
                Instant.now(),
                AuthenticationAuditEvent.Outcome.SUCCESS,
                currentActorIdentityId(),
                saved.getId(),
                "IDENTITY_PROVISIONED"
        ));
        return saved;
    }

    private String normalizeUsername(String username) {
        if (username == null) {
            throw new BusinessException("A valid username is required.");
        }
        String normalized = username.trim().toLowerCase(Locale.ROOT);
        if (!USERNAME_PATTERN.matcher(normalized).matches()) {
            throw new BusinessException("Username must contain 3-50 letters, digits, dots, underscores, or hyphens.");
        }
        return normalized;
    }

    private void validateProvisioningPassword(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < MIN_PASSWORD_LENGTH
                || rawPassword.getBytes(StandardCharsets.UTF_8).length > MAX_BCRYPT_PASSWORD_BYTES) {
            throw new BusinessException("Password must be at least 12 characters and at most 72 UTF-8 bytes.");
        }
    }

    private BadCredentialsException invalidCredentials() {
        return new BadCredentialsException("Invalid username or password.");
    }

    private void publishAuthenticationFailure() {
        eventPublisher.publishEvent(new AuthenticationAuditEvent(
                Instant.now(),
                AuthenticationAuditEvent.Outcome.FAILURE,
                null,
                null,
                "AUTHENTICATION_FAILED"
        ));
    }

    private Long currentActorIdentityId() {
        var authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user.getId();
        }
        return null;
    }
}
