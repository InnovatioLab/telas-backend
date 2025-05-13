package com.telas.infra.security.services;

import com.telas.entities.Client;
import com.telas.infra.security.model.TokenData;

public interface TokenService {
    String generateToken(Client client);

    TokenData validateToken(String token);
}
