package com.telas.infra.security.services.impl;

import com.telas.entities.Client;
import com.telas.entities.Contact;
import com.telas.enums.DefaultStatus;
import com.telas.enums.Role;
import com.telas.infra.security.model.TokenData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class TokenServiceImplTest {

    private TokenServiceImpl tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenServiceImpl();
        ReflectionTestUtils.setField(tokenService, "secret", "test-secret-for-jwt-validation");
    }

    @Test
    void validateToken_shouldAcceptUuidIdClaim() {
        Client client = buildClient();

        String token = tokenService.generateToken(client);
        TokenData tokenData = tokenService.validateToken(token);

        assertNotNull(tokenData);
        assertEquals(client.getId(), tokenData.getId());
        assertEquals(client.getContact().getEmail(), tokenData.getEmail());
    }

    @Test
    void validateToken_shouldReturnNullForInvalidToken() {
        assertNull(tokenService.validateToken("invalid-token"));
    }

    private static Client buildClient() {
        Client client = new Client();
        client.setId(UUID.fromString("3520e5f2-ce42-41ad-93d9-4a6310ada639"));
        client.setBusinessName("Admin DEV");
        client.setRole(Role.DEVELOPER);
        client.setStatus(DefaultStatus.ACTIVE);
        Contact contact = new Contact();
        contact.setEmail("administrator-developer@telas-ads.com");
        client.setContact(contact);
        return client;
    }
}
