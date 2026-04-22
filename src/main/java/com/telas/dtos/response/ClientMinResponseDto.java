package com.telas.dtos.response;

import com.telas.entities.Address;
import com.telas.entities.Client;
import com.telas.entities.Contact;
import com.telas.enums.DefaultStatus;
import com.telas.enums.Role;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
public final class ClientMinResponseDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 5288515525105234502L;

    private final UUID id;

    private final String businessName;

    private final Role role;

    private final String industry;

    private final String websiteUrl;

    private final DefaultStatus status;

    private final Contact contact;

    private final String partnerAddressSummary;

    private final int adsCount;

    private final Instant createdAt;

    private final Instant updatedAt;

    private final boolean reactivatableByCurrentUser;

    public ClientMinResponseDto(Client entity) {
        this(entity, null, false);
    }

    public ClientMinResponseDto(Client entity, UUID viewerClientId, boolean hasDeactivatePermission) {
        id = entity.getId();
        businessName = entity.getBusinessName();
        role = entity.getRole();
        industry = entity.getIndustry();
        websiteUrl = entity.getWebsiteUrl();
        status = entity.getStatus();
        contact = entity.getContact();
        partnerAddressSummary = resolvePartnerAddressSummary(entity);
        adsCount = resolveAdsCount(entity);
        createdAt = entity.getCreatedAt();
        updatedAt = entity.getUpdatedAt();
        reactivatableByCurrentUser = computeReactivatable(entity, viewerClientId, hasDeactivatePermission);
    }

    private static boolean computeReactivatable(
            Client entity, UUID viewerId, boolean hasDeactivatePermission) {
        if (!hasDeactivatePermission || viewerId == null) {
            return false;
        }
        if (!DefaultStatus.INACTIVE.equals(entity.getStatus())) {
            return false;
        }
        if (entity.getInactiveByClientId() == null) {
            return false;
        }
        return entity.getInactiveByClientId().equals(viewerId);
    }

    private static String resolvePartnerAddressSummary(Client entity) {
        if (!Role.PARTNER.equals(entity.getRole())
                || entity.getAddresses() == null
                || entity.getAddresses().isEmpty()) {
            return null;
        }
        return entity.getAddresses().stream()
                .filter(Objects::nonNull)
                .map(Address::getCoordinatesParams)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(" · "));
    }

    private static int resolveAdsCount(Client entity) {
        if (!Role.PARTNER.equals(entity.getRole()) || entity.getAds() == null) {
            return 0;
        }
        return entity.getAds().size();
    }
}
