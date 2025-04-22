package com.marketingproject.infra.security.services.impl;

import com.marketingproject.entities.Client;
import com.marketingproject.infra.exceptions.ResourceNotFoundException;
import com.marketingproject.infra.security.model.AuthenticatedUser;
import com.marketingproject.infra.security.services.AuthService;
import com.marketingproject.repositories.ClientRepository;
import com.marketingproject.shared.constants.valitation.ClientValidationMessages;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final ClientRepository clientRepository;

    @Transactional(readOnly = true)
    @Override
    public UserDetails loadUserByUsername(String identificationNumber) throws UsernameNotFoundException {
        try {
            Client client = clientRepository.findActiveByIdentificationNumber(identificationNumber)
                    .orElseThrow(() -> new UsernameNotFoundException(ClientValidationMessages.USER_NOT_FOUND));
            return new AuthenticatedUser(client);
        } catch (UsernameNotFoundException e) {
            throw new ResourceNotFoundException(ClientValidationMessages.USER_NOT_FOUND);
        }
    }
}
