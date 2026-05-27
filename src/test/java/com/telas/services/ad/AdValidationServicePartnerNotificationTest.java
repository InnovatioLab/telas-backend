package com.telas.services.ad;

import com.telas.entities.Ad;
import com.telas.entities.Client;
import com.telas.enums.NotificationReference;
import com.telas.enums.Role;
import com.telas.services.notification.AdminAdsNotificationService;
import com.telas.shared.utils.ClientPortalLinkResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdValidationServicePartnerNotificationTest {

    @Mock
    private AdminAdsNotificationService adminAdsNotificationService;
    @Mock
    private ClientPortalLinkResolver clientPortalLinkResolver;

    @InjectMocks
    private AdValidationService adValidationService;

    @Test
    void notifyAdminsClientApprovedAd_whenPartner_setsActorTypePartner() throws Exception {
        Client partner = new Client();
        partner.setId(UUID.randomUUID());
        partner.setBusinessName("Partner Co");
        partner.setRole(Role.PARTNER);

        Ad ad = new Ad();
        ad.setName("Summer Ad");
        ad.setClient(partner);

        when(clientPortalLinkResolver.adminClientMessagesLink(partner.getId()))
                .thenReturn("https://front.test/admin/clients/" + partner.getId());

        var method = AdValidationService.class.getDeclaredMethod("notifyAdminsClientApprovedAd", Ad.class);
        method.setAccessible(true);
        method.invoke(adValidationService, ad);

        ArgumentCaptor<Map<String, String>> params = ArgumentCaptor.forClass(Map.class);
        verify(adminAdsNotificationService).notifyAdmins(
                eq(NotificationReference.ADMIN_CLIENT_AD_APPROVED),
                params.capture());
        assertThat(params.getValue().get("actorType")).isEqualTo("partner");
        assertThat(params.getValue().get("clientName")).isEqualTo("Partner Co");
    }
}
