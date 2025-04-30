package com.marketingproject.infra.security.services.impl;

import com.marketingproject.entities.Client;
import com.marketingproject.enums.CodeType;
import com.marketingproject.enums.DefaultStatus;
import com.marketingproject.infra.exceptions.BusinessRuleException;
import com.marketingproject.infra.security.model.AuthenticatedUser;
import com.marketingproject.infra.security.model.LoginRequestDto;
import com.marketingproject.infra.security.model.PasswordRequestDto;
import com.marketingproject.infra.security.model.PasswordUpdateRequestDto;
import com.marketingproject.infra.security.services.AuthService;
import com.marketingproject.infra.security.services.AuthenticatedUserService;
import com.marketingproject.infra.security.services.TokenService;
import com.marketingproject.services.ClientService;
import com.marketingproject.shared.constants.valitation.AuthValidationMessageConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
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
        AuthenticatedUser authenticatedUser = (AuthenticatedUser) userDetailsService.loadUserByUsername(requestDto.getUsername());

        if (authenticatedUser == null) {
            throw new BusinessRuleException(AuthValidationMessageConstants.INVALID_CREDENTIALS);
        }

        if (!passwordEncoder.matches(requestDto.getPassword(), authenticatedUser.getPassword())) {
            throw new BusinessRuleException(AuthValidationMessageConstants.INVALID_CREDENTIALS);
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(authenticatedUser, null, authenticatedUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        return tokenService.generateToken(authenticatedUser.client());
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

        if (DefaultStatus.ACTIVE.equals(client.getStatus()) && CodeType.PASSWORD.equals(client.getVerificationCode().getCodeType())) {
            clientService.updatePassword(request, authClient);
        }
    }
}
