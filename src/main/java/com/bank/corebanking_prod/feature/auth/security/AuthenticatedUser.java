package com.bank.corebanking_prod.feature.auth.security;

import com.bank.corebanking_prod.feature.auth.model.enums.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class AuthenticatedUser implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final UserRole role;
    private final Long customerId;
    private final boolean active;
    private final Collection<? extends GrantedAuthority> authorities;

    public AuthenticatedUser(
            Long id,
            String username,
            String password,
            UserRole role,
            Long customerId,
            boolean active) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.customerId = customerId;
        this.active = active;
        this.authorities = List.of(new SimpleGrantedAuthority(role.getAuthority()));
    }

    public Long getId() {
        return id;
    }

    public UserRole getRole() {
        return role;
    }

    public Long getCustomerId() {
        return customerId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
