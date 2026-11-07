package com.example.demo.shared.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public record AuthenticatedUser(
        Long id,
        String email,
        String passwordHash,
        String role,
        boolean enabled,
        int tokenVersion
) implements UserDetails {

    public static final String ADMIN_ROLE = "ADMIN";

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public boolean isAdmin() {
        return ADMIN_ROLE.equals(role);
    }

    @Override
    public String toString() {
        return "AuthenticatedUser[id=" + id + ", role=" + role + ", enabled=" + enabled + "]";
    }
}
