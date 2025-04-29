package com.marketingproject.infra.security.services.impl;

import com.marketingproject.entities.Client;
import com.marketingproject.infra.exceptions.ForbiddenException;
import com.marketingproject.infra.exceptions.UnauthorizedException;
import com.marketingproject.infra.security.model.AuthenticatedUser;
import com.marketingproject.infra.security.services.AuthenticatedUserService;
import com.marketingproject.shared.constants.valitation.AuthValidationMessageConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticatedUserServiceImpl implements AuthenticatedUserService {

    @Transactional(readOnly = true)
    @Override
    public AuthenticatedUser getLoggedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            throw new UnauthorizedException(AuthValidationMessageConstants.ERROR_NO_AUTHENTICATION);
        }

        AuthenticatedUser authenticatedUser = (AuthenticatedUser) authentication.getPrincipal();
        return authenticatedUser;
    }

    @Transactional(readOnly = true)
    @Override
    public AuthenticatedUser validateSelfOrAdmin(UUID id) {
        Client loggedClient = getLoggedUser().client();
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(loggedClient);

        if (loggedClient.isAdmin() || loggedClient.getId().equals(id)) {
            return authenticatedUser;
        }

        throw new ForbiddenException(AuthValidationMessageConstants.ERROR_NO_PERMISSION);
    }

    @Transactional(readOnly = true)
    @Override
    public AuthenticatedUser validateAdmin() {
        Client loggedClient = getLoggedUser().client();
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(loggedClient);

        if (loggedClient.isAdmin()) {
            return authenticatedUser;
        }

        throw new ForbiddenException(AuthValidationMessageConstants.ERROR_NO_PERMISSION);

    }
}
