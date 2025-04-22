package com.marketingproject.infra.security.services;

import com.marketingproject.infra.security.model.AuthenticatedUser;

public interface AuthenticatedUserService {
    AuthenticatedUser getLoggedUser();
}
