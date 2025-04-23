package com.marketingproject.infra.security.services;

import com.marketingproject.entities.Client;

public interface TokenService {
    String generateToken(Client client);

    String validateToken(String token);
}
