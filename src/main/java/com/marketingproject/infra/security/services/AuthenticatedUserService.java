package com.marketingproject.infra.security.services;

import com.marketingproject.infra.security.model.AuthenticatedUser;

import java.util.UUID;

public interface AuthenticatedUserService {
    AuthenticatedUser getLoggedUser();

    AuthenticatedUser validateSelfOrAdmin(UUID id);

    AuthenticatedUser validateAdmin();
}
