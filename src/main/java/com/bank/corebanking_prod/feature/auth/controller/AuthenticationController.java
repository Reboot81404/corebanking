package com.bank.corebanking_prod.feature.auth.controller;

import com.bank.corebanking_prod.feature.auth.model.dto.LoginRequest;
import com.bank.corebanking_prod.feature.auth.model.dto.ProvisionUserRequest;
import com.bank.corebanking_prod.feature.auth.model.dto.ProvisionedUserResponse;
import com.bank.corebanking_prod.feature.auth.model.dto.TokenResponse;
import com.bank.corebanking_prod.feature.auth.model.entity.UserIdentityEntity;
import com.bank.corebanking_prod.feature.auth.security.LoginAttemptRateLimiter;
import com.bank.corebanking_prod.feature.auth.service.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final LoginAttemptRateLimiter loginAttemptRateLimiter;

    public AuthenticationController(
            AuthenticationService authenticationService,
            LoginAttemptRateLimiter loginAttemptRateLimiter) {
        this.authenticationService = authenticationService;
        this.loginAttemptRateLimiter = loginAttemptRateLimiter;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpServletRequest) {
        String clientAddress = httpServletRequest.getRemoteAddr();
        loginAttemptRateLimiter.checkAllowed(clientAddress);
        try {
            TokenResponse response = authenticationService.login(request);
            loginAttemptRateLimiter.recordSuccess(clientAddress);
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException exception) {
            loginAttemptRateLimiter.recordFailure(clientAddress);
            throw exception;
        }
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProvisionedUserResponse> provisionUser(
            @Valid @RequestBody ProvisionUserRequest request) {
        UserIdentityEntity identity = authenticationService.provisionUser(
                request.getUsername(),
                request.getPassword(),
                request.getRole(),
                request.getCustomerId()
        );
        ProvisionedUserResponse response = new ProvisionedUserResponse(
                identity.getId(),
                identity.getUsername(),
                identity.getRole(),
                identity.getStatus(),
                identity.getCustomerId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
