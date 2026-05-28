package com.telas.shared.utils;

import com.telas.entities.Client;
import com.telas.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientPortalLinkResolverTest {

    private final ClientPortalLinkResolver resolver = new ClientPortalLinkResolver();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(resolver, "frontBaseUrl", "https://front.test");
    }

    @Test
    void clientAdsTabLink_whenPartner_returnsScreensPath() {
        Client partner = partnerClient();

        assertEquals("https://front.test/partner/screens", resolver.clientAdsTabLink(partner));
    }

    @Test
    void clientAdsTabLink_whenNotPartner_returnsMyTelasAdsTab() {
        Client client = new Client();

        assertEquals("https://front.test/client/my-telas?tab=ads", resolver.clientAdsTabLink(client));
    }

    @Test
    void clientPartnerAdsReviewLink_whenPartner_returnsPartnerAdsPath() {
        Client partner = partnerClient();

        assertEquals("https://front.test/partner/ads-review", resolver.clientPartnerAdsReviewLink(partner));
    }

    @Test
    void clientApprovedConfirmationLink_whenPartnerLiveOnScreen_returnsScreensPath() {
        Client partner = partnerClient();

        assertEquals(
                "https://front.test/partner/screens",
                resolver.clientApprovedConfirmationLink(partner, true));
    }

    @Test
    void adminClientMessagesLink_includesClientId() {
        UUID clientId = UUID.randomUUID();

        assertEquals(
                "https://front.test/admin/clients/" + clientId + "/messages",
                resolver.adminClientMessagesLink(clientId));
    }

    private static Client partnerClient() {
        Client client = new Client();
        client.setId(UUID.randomUUID());
        client.setRole(Role.PARTNER);
        return client;
    }
}
