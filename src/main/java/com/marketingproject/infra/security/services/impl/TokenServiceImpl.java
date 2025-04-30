package com.marketingproject.infra.security.services.impl;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.marketingproject.entities.Client;
import com.marketingproject.infra.security.model.AuthenticatedUser;
import com.marketingproject.infra.security.services.TokenService;
import com.marketingproject.shared.constants.SharedConstants;
import com.marketingproject.shared.constants.valitation.AuthValidationMessageConstants;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {
    private final Logger log = LogManager.getLogger(TokenServiceImpl.class);

    @Value("${api.security.token.secret}")
    private String secret;

    @Override
    @Transactional
    public String generateToken(Client client) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            AuthenticatedUser user = new AuthenticatedUser(client);

            Map<String, Object> payload = Map.of(
                    SharedConstants.PERMISSIONS, new ArrayList<>(getPermissions(user)),
                    "businessName", client.getBusinessName(),
                    "identificationNumber", client.getIdentificationNumber()
            );

            return JWT.create()
                    .withIssuer(SharedConstants.PROJECT_NAME)
                    .withSubject(client.getId().toString())
                    .withPayload(payload)
                    .withExpiresAt(genExpirationDate())
                    .sign(algorithm);
        } catch (JWTCreationException ex) {
            log.error("Error while generating JWT token, message: {}", ex.getMessage());
            throw new JWTCreationException(AuthValidationMessageConstants.ERROR_TOKEN_GENERATION, ex);
        }
    }

    @Override
    @Transactional
    public String validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer(SharedConstants.PROJECT_NAME)
                    .build()
                    .verify(token)
                    .getClaim("identificationNumber")
                    .asString();
        } catch (JWTVerificationException ex) {
            log.error("Error while verifying JWT token, message: {}", ex.getMessage());
            return "";
        }
    }


    Instant genExpirationDate() {
//        America/New_York
        return LocalDateTime.now(ZoneOffset.UTC).plusMinutes(30L).toInstant(ZoneOffset.UTC);
    }


    Set<String> getPermissions(AuthenticatedUser authenticatedUser) {
        Set<String> set = new HashSet<>();
        authenticatedUser.getAuthorities().forEach(permission ->
                set.add(String.valueOf(permission))
        );
        return set;
    }
}
