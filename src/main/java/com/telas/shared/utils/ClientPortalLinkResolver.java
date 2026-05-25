package com.telas.shared.utils;

import com.telas.entities.Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ClientPortalLinkResolver {

    @Value("${front.base.url}")
    private String frontBaseUrl;

    public String clientAdsTabLink(Client client) {
        if (client != null && client.isPartner()) {
            return frontBaseUrl + "/client/screens";
        }
        return frontBaseUrl + "/client/my-telas?tab=ads";
    }

    public String clientPartnerAdsReviewLink(Client client) {
        if (client != null && client.isPartner()) {
            return frontBaseUrl + "/client/partner-ads";
        }
        return clientAdsTabLink(client);
    }

    public String clientApprovedConfirmationLink(Client client, boolean liveOnScreen) {
        if (client != null && client.isPartner()) {
            return liveOnScreen ? clientScreensLink() : frontBaseUrl + "/client/partner-ads";
        }
        return frontBaseUrl + "/client/my-telas?tab=ads";
    }

    public String clientScreensLink() {
        return frontBaseUrl + "/client/screens";
    }

    public String adminClientMessagesLink(UUID clientId) {
        return frontBaseUrl + "/admin/clients/" + clientId + "/messages";
    }

    public String adminAdsLink() {
        return frontBaseUrl + "/admin/ads";
    }

    public String adminClientLink(UUID clientId) {
        return frontBaseUrl + "/admin/clients/" + clientId;
    }

    public String adminAdRequestsLink() {
        return frontBaseUrl + "/admin/ad-requests";
    }

    public String partnerFlag(Client client) {
        return client != null && client.isPartner() ? "true" : "false";
    }
}
