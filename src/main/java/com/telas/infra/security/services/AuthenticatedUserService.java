package com.telas.infra.security.services;

import com.telas.infra.security.model.AuthenticatedUser;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.UUID;

public interface AuthenticatedUserService {
    AuthenticatedUser getLoggedUser();

    AuthenticatedUser validateSelfOrAdmin(UUID id);

    AuthenticatedUser validateAdmin();

    void verifyTermsAccepted(UserDetails user);
}
