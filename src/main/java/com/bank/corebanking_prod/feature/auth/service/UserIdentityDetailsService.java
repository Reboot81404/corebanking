package com.bank.corebanking_prod.feature.auth.service;

import com.bank.corebanking_prod.feature.auth.model.entity.UserIdentityEntity;
import com.bank.corebanking_prod.feature.auth.model.enums.UserStatus;
import com.bank.corebanking_prod.feature.auth.repository.UserIdentityRepository;
import com.bank.corebanking_prod.feature.auth.security.AuthenticatedUser;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class UserIdentityDetailsService implements UserDetailsService {

    private final UserIdentityRepository userIdentityRepository;

    public UserIdentityDetailsService(UserIdentityRepository userIdentityRepository) {
        this.userIdentityRepository = userIdentityRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null) {
            throw new UsernameNotFoundException("Invalid credentials.");
        }
        String normalized = username.trim().toLowerCase(Locale.ROOT);
        UserIdentityEntity entity = userIdentityRepository.findByUsername(normalized)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials."));

        boolean active = entity.getStatus() == UserStatus.ACTIVE;
        return new AuthenticatedUser(
                entity.getId(),
                entity.getUsername(),
                entity.getPasswordHash(),
                entity.getRole(),
                entity.getCustomerId(),
                active
        );
    }
}
