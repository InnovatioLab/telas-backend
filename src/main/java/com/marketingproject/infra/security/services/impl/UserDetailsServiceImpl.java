package com.marketingproject.infra.security.services.impl;

import com.marketingproject.infra.security.model.AuthenticatedUser;
import com.marketingproject.repositories.ClientRepository;
import com.marketingproject.shared.constants.valitation.ClientValidationMessages;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final ClientRepository clientRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identificationNumber) throws UsernameNotFoundException {
        return clientRepository.findActiveByIdentificationNumber(identificationNumber)
                .map(AuthenticatedUser::new)
                .orElseThrow(() -> new UsernameNotFoundException(ClientValidationMessages.USER_NOT_FOUND));
    }

}
