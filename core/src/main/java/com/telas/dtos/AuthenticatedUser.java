package com.telas.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.telas.entities.Client;
import com.telas.enums.DefaultStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public record AuthenticatedUser(Client client) implements UserDetails {
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @JsonIgnore
    @Override
    public String getPassword() {
        return client.getPassword();
    }

    @Override
    public String getUsername() {
        return client.getContact().getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @JsonIgnore
    @Override
    public boolean isEnabled() {
        return DefaultStatus.ACTIVE.equals(client.getStatus());
    }

    public boolean isAdmin() {
        return client.isAdmin();
    }

    public boolean isTermsAccepted() {
        return client.isTermsAccepted();
    }
}
