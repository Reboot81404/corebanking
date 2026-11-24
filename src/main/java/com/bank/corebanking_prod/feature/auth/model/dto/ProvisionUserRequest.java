package com.bank.corebanking_prod.feature.auth.model.dto;

import com.bank.corebanking_prod.feature.auth.model.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ProvisionUserRequest {

    @NotBlank(message = "Username is required.")
    @Size(min = 3, max = 50, message = "Username must contain between 3 and 50 characters.")
    @Pattern(regexp = "[A-Za-z0-9._-]+", message = "Username may contain only letters, digits, dot, underscore, and hyphen.")
    private String username;

    @NotBlank(message = "Password is required.")
    @Size(min = 4, max = 72, message = "Password must contain between 4 and 72 characters.")
    private String password;

    @NotNull(message = "Role is required.")
    private UserRole role;

    private Long customerId;

    public ProvisionUserRequest() {
    }

    public ProvisionUserRequest(String username, String password, UserRole role, Long customerId) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.customerId = customerId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }
}
