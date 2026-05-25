package com.telas.infra.security.services.impl;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.telas.entities.Client;
import com.telas.infra.security.model.AuthenticatedUser;
import com.telas.infra.security.model.TokenData;
import com.telas.infra.security.services.TokenService;
import com.telas.shared.constants.SharedConstants;
import com.telas.shared.constants.valitation.AuthValidationMessageConstants;
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
import java.util.Set;
import java.util.UUID;

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

            return JWT.create()
                    .withIssuer(SharedConstants.PROJECT_NAME)
                    .withSubject(client.getId().toString())
                    .withClaim(SharedConstants.PERMISSIONS, new ArrayList<>(getPermissions(user)))
                    .withClaim("id", client.getId().toString())
                    .withClaim("businessName", client.getBusinessName())
                    .withClaim("email", client.getContact().getEmail())
                    .withExpiresAt(genExpirationDate())
                    .sign(algorithm);
        } catch (JWTCreationException ex) {
            log.error("Error while generating JWT token, message: {}", ex.getMessage());
            throw new JWTCreationException(AuthValidationMessageConstants.ERROR_TOKEN_GENERATION, ex);
        }
    }

    @Override
    @Transactional
    public TokenData validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            var decodedJWT = JWT.require(algorithm)
                    .withIssuer(SharedConstants.PROJECT_NAME)
                    .build()
                    .verify(token);

            String email = decodedJWT.getClaim("email").asString();
            UUID clientId = resolveClientId(decodedJWT.getClaim("id"));

            return new TokenData(clientId, email);
        } catch (JWTVerificationException ex) {
            log.error("Error while verifying JWT token, message: {}", ex.getMessage());
            return null;
        }
    }


    Instant genExpirationDate() {
        return LocalDateTime.now(ZoneOffset.UTC).plusMinutes(30L).toInstant(ZoneOffset.UTC);
    }


    Set<String> getPermissions(AuthenticatedUser authenticatedUser) {
        Set<String> set = new HashSet<>();
        authenticatedUser.getAuthorities().forEach(permission ->
                set.add(String.valueOf(permission))
        );
        return set;
    }

    private static UUID resolveClientId(com.auth0.jwt.interfaces.Claim idClaim) {
        if (idClaim == null || idClaim.isNull()) {
            return null;
        }

        String idAsString = idClaim.asString();
        if (idAsString == null || idAsString.isBlank()) {
            return null;
        }

        return UUID.fromString(idAsString);
    }
}
