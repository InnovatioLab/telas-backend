package com.telas.infra.security.services.impl;

import com.telas.entities.Client;
import com.telas.enums.DefaultStatus;
import com.telas.enums.Permission;
import com.telas.infra.exceptions.ForbiddenException;
import com.telas.infra.exceptions.UnauthorizedException;
import com.telas.infra.security.model.AuthenticatedUser;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.repositories.ClientRepository;
import com.telas.services.PermissionService;
import com.telas.shared.constants.valitation.AuthValidationMessageConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticatedUserServiceImpl implements AuthenticatedUserService {
    private final ClientRepository clientRepository;
    private final PermissionService permissionService;

    @Transactional(readOnly = true)
    @Override
    public AuthenticatedUser getLoggedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            throw new UnauthorizedException(AuthValidationMessageConstants.ERROR_NO_AUTHENTICATION);
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof AuthenticatedUser) {
            AuthenticatedUser authenticatedUser = (AuthenticatedUser) principal;
            Client client = clientRepository.findActiveById(authenticatedUser.client().getId())
                    .orElseThrow(() -> new UnauthorizedException(AuthValidationMessageConstants.ERROR_NO_AUTHENTICATION));
            return new AuthenticatedUser(client);
        }

        String email = null;
        
        if (principal instanceof UserDetails) {
            email = ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            email = (String) principal;
        }

        if (email != null) {
            Client client = clientRepository.findByEmail(email)
                    .filter(data -> DefaultStatus.ACTIVE.equals(data.getStatus()))
                    .orElseThrow(() -> new UnauthorizedException(AuthValidationMessageConstants.ERROR_NO_AUTHENTICATION));
            return new AuthenticatedUser(client);
        }

        throw new UnauthorizedException(AuthValidationMessageConstants.ERROR_NO_AUTHENTICATION);
    }

    @Transactional(readOnly = true)
    @Override
    public AuthenticatedUser validateSelfOrAdmin(UUID id) {
        Client loggedClient = getLoggedUser().client();
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(loggedClient);

        if (loggedClient.isPrivilegedPanelUser() || loggedClient.getId().equals(id)) {
            return authenticatedUser;
        }

        throw new ForbiddenException(AuthValidationMessageConstants.ERROR_NO_PERMISSION);
    }

    @Transactional(readOnly = true)
    @Override
    public AuthenticatedUser validateAdmin() {
        Client loggedClient = getLoggedUser().client();
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(loggedClient);

        if (loggedClient.isPrivilegedPanelUser()) {
            return authenticatedUser;
        }

        throw new ForbiddenException(AuthValidationMessageConstants.ERROR_NO_PERMISSION);
    }

    @Transactional(readOnly = true)
    @Override
    public AuthenticatedUser validateDeveloper() {
        Client loggedClient = getLoggedUser().client();
        if (loggedClient.isDeveloper()) {
            return new AuthenticatedUser(loggedClient);
        }
        throw new ForbiddenException(AuthValidationMessageConstants.ERROR_NO_PERMISSION);
    }

    @Override
    @Transactional(readOnly = true)
    public void validatePermission(Permission permission) {
        Client loggedClient = getLoggedUser().client();
        if (!permissionService.hasPermission(loggedClient, permission)) {
            throw new ForbiddenException(AuthValidationMessageConstants.ERROR_NO_PERMISSION);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AuthenticatedUser validateActiveSubscription() {
        Client loggedClient = getLoggedUser().client();
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(loggedClient);

        if (loggedClient.hasActiveSubscription() || loggedClient.isPrivilegedPanelUser()) {
            return authenticatedUser;
        }

        throw new ForbiddenException(AuthValidationMessageConstants.ERROR_NO_ACTIVE_SUBSCRIPTION);
    }

    @Override
    @Transactional(readOnly = true)
    public void verifyTermsAccepted(UserDetails user) {
        AuthenticatedUser authenticatedUser = (AuthenticatedUser) user;

        if (!authenticatedUser.isAdmin()
                && !authenticatedUser.isDeveloper()
                && !authenticatedUser.isTermsAccepted()) {
            throw new ForbiddenException(AuthValidationMessageConstants.ERROR_TERMS_NOT_ACCEPTED);
        }
    }
}
