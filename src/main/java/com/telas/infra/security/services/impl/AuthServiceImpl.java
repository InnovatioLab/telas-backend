package com.telas.infra.security.services.impl;

import com.telas.entities.Client;
import com.telas.enums.CodeType;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.exceptions.UnauthorizedException;
import com.telas.infra.security.model.AuthenticatedUser;
import com.telas.infra.security.model.LoginRequestDto;
import com.telas.infra.security.model.PasswordRequestDto;
import com.telas.infra.security.model.PasswordUpdateRequestDto;
import com.telas.infra.security.services.AuthService;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.infra.security.services.TokenService;
import com.telas.services.ClientService;
import com.telas.shared.constants.valitation.AuthValidationMessageConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
  private final ClientService clientService;
  private final PasswordEncoder passwordEncoder;
  private final TokenService tokenService;
  private final AuthenticatedUserService authenticatedUserService;
  private final UserDetailsServiceImpl userDetailsService;

  @Override
  @Transactional(readOnly = true)
  public String login(LoginRequestDto requestDto) {
    try {
      AuthenticatedUser authenticatedUser = (AuthenticatedUser) userDetailsService.loadUserByUsername(requestDto.getUsername());

      if (!passwordEncoder.matches(requestDto.getPassword(), authenticatedUser.getPassword())) {
        throw new BusinessRuleException(AuthValidationMessageConstants.INVALID_CREDENTIALS);
      }

      UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(authenticatedUser, null, authenticatedUser.getAuthorities());
      SecurityContextHolder.getContext().setAuthentication(authentication);

      return tokenService.generateToken(authenticatedUser.client());
    } catch (UsernameNotFoundException e) {
      throw new UnauthorizedException(AuthValidationMessageConstants.INVALID_CREDENTIALS);
    }
  }

  @Transactional
  @Override
  public void sendPasswordRecoveryCode(String identificationNumber) {
    clientService.sendResetPasswordCode(identificationNumber);
  }

  @Transactional
  @Override
  public void resetPassword(String identificationNumber, PasswordRequestDto request) {
    clientService.resetPassword(identificationNumber, request);
  }

  @Transactional
  @Override
  public void updatePassword(PasswordUpdateRequestDto request) {
    request.validate();
    AuthenticatedUser authClient = authenticatedUserService.getLoggedUser();
    Client client = authClient.client();

    if (!CodeType.PASSWORD.equals(client.getVerificationCode().getCodeType())) {
      throw new BusinessRuleException(AuthValidationMessageConstants.INVALID_CODE_TYPE_FOR_PASSWORD_UPDATE);
    }

    clientService.updatePassword(request, authClient);
  }
}
