package com.telas.infra.security.services;

import com.telas.entities.Client;

public interface TokenService {
    String generateToken(Client client);

    String validateToken(String token);
}
