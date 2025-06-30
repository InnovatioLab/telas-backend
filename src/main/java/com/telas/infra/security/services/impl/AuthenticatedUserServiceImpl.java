package com.telas.infra.security.services.impl;

import com.telas.entities.Client;
import com.telas.infra.exceptions.ForbiddenException;
import com.telas.infra.exceptions.UnauthorizedException;
import com.telas.infra.security.model.AuthenticatedUser;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.repositories.ClientRepository;
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

  @Transactional(readOnly = true)
  @Override
  public AuthenticatedUser getLoggedUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null) {
      throw new UnauthorizedException(AuthValidationMessageConstants.ERROR_NO_AUTHENTICATION);
    }

    AuthenticatedUser authenticatedUser = (AuthenticatedUser) authentication.getPrincipal();

    Client client = clientRepository.findActiveById(authenticatedUser.client().getId())
            .orElseThrow(() -> new UnauthorizedException(AuthValidationMessageConstants.ERROR_NO_AUTHENTICATION));

    return new AuthenticatedUser(client);
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

  @Override
  @Transactional(readOnly = true)
  public AuthenticatedUser validateActiveSubscription() {
    Client loggedClient = getLoggedUser().client();
    AuthenticatedUser authenticatedUser = new AuthenticatedUser(loggedClient);

    if (loggedClient.hasActiveSubscription() || loggedClient.isAdmin()) {
      return authenticatedUser;
    }

    throw new ForbiddenException(AuthValidationMessageConstants.ERROR_NO_ACTIVE_SUBSCRIPTION);
  }

  @Override
  @Transactional(readOnly = true)
  public void verifyTermsAccepted(UserDetails user) {
    AuthenticatedUser authenticatedUser = (AuthenticatedUser) user;

    if (!authenticatedUser.isAdmin() && !authenticatedUser.isTermsAccepted()) {
      throw new ForbiddenException(AuthValidationMessageConstants.ERROR_TERMS_NOT_ACCEPTED);
    }
  }
}
