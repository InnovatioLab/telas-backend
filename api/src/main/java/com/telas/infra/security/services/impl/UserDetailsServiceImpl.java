package com.telas.infra.security.services.impl;

import com.telas.enums.DefaultStatus;
import com.telas.repositories.ClientRepository;
import com.telas.shared.constants.valitation.ClientValidationMessages;
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
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return clientRepository.findByEmail(email)
                .filter(data -> DefaultStatus.ACTIVE.equals(data.getStatus()))
                .map(AuthenticatedUser::new)
                .orElseThrow(() -> new UsernameNotFoundException(ClientValidationMessages.USER_NOT_FOUND));

    }

}
