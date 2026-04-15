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

    private final Instant createdAt;

    private final Instant updatedAt;

    public ClientMinResponseDto(Client entity) {
        id = entity.getId();
        businessName = entity.getBusinessName();
        role = entity.getRole();
        industry = entity.getIndustry();
        websiteUrl = entity.getWebsiteUrl();
        status = entity.getStatus();
        contact = entity.getContact();
        partnerAddressSummary = resolvePartnerAddressSummary(entity);
        createdAt = entity.getCreatedAt();
        updatedAt = entity.getUpdatedAt();
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
}
