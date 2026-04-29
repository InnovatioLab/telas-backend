package com.telas.dtos.response;

import com.telas.enums.AdOperationUrgencyLevel;
import com.telas.enums.AdValidationType;
import com.telas.enums.SubscriptionStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class AdminAdOperationRowDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private UUID adId;
    private String adName;
    private AdValidationType validation;
    private UUID advertiserId;
    private String advertiserBusinessName;
    private UUID partnerId;
    private String partnerBusinessName;
    private String screenAddressSummary;
    private UUID monitorId;
    private String boxIp;
    private Instant subscriptionEndsAt;
    private SubscriptionStatus subscriptionStatus;
    private String operationalStage;
    private AdOperationUrgencyLevel urgencyLevel;
    private Long daysUntilExpiry;

    public AdminAdOperationRowDto(
            UUID adId,
            String adName,
            AdValidationType validation,
            UUID advertiserId,
            String advertiserBusinessName,
            UUID partnerId,
            String partnerBusinessName,
            String street,
            String city,
            String state,
            String zipCode,
            UUID monitorId,
            String boxIp,
            Instant subscriptionEndsAt,
            SubscriptionStatus subscriptionStatus) {
        this.adId = adId;
        this.adName = adName;
        this.validation = validation;
        this.advertiserId = advertiserId;
        this.advertiserBusinessName = advertiserBusinessName;
        this.partnerId = partnerId;
        this.partnerBusinessName = partnerBusinessName;
        this.screenAddressSummary = String.join(", ", street, city, state, zipCode);
        this.monitorId = monitorId;
        this.boxIp = boxIp;
        this.subscriptionEndsAt = subscriptionEndsAt;
        this.subscriptionStatus = subscriptionStatus;
        applyOperationalDerivedFields();
    }

    public void applyOperationalDerivedFields() {
        Instant now = Instant.now();
        if (subscriptionEndsAt != null) {
            daysUntilExpiry = ChronoUnit.DAYS.between(now, subscriptionEndsAt);
        } else {
            daysUntilExpiry = null;
        }
        urgencyLevel = resolveUrgency(subscriptionEndsAt, subscriptionStatus, now);
        operationalStage = resolveStage();
    }

    private static AdOperationUrgencyLevel resolveUrgency(
            Instant endsAt, SubscriptionStatus status, Instant now) {
        if (endsAt == null) {
            return AdOperationUrgencyLevel.NEUTRAL;
        }
        if (!SubscriptionStatus.ACTIVE.equals(status)) {
            return AdOperationUrgencyLevel.NEUTRAL;
        }
        long days = ChronoUnit.DAYS.between(now, endsAt);
        if (days < 0) {
            return AdOperationUrgencyLevel.RED;
        }
        if (days <= 3) {
            return AdOperationUrgencyLevel.RED;
        }
        if (days <= 14) {
            return AdOperationUrgencyLevel.YELLOW;
        }
        return AdOperationUrgencyLevel.GREEN;
    }

    private String resolveStage() {
        if (AdValidationType.PENDING.equals(validation)) {
            return "PENDING_VALIDATION";
        }
        if (AdValidationType.REJECTED.equals(validation)) {
            return "REJECTED";
        }
        if (boxIp != null && !boxIp.isBlank()) {
            return "IN_BOX";
        }
        if (subscriptionStatus == null) {
            return "APPROVED_NO_ACTIVE_SUBSCRIPTION";
        }
        if (SubscriptionStatus.EXPIRED.equals(subscriptionStatus)) {
            return "SUBSCRIPTION_EXPIRED";
        }
        if (SubscriptionStatus.ACTIVE.equals(subscriptionStatus)) {
            if (subscriptionEndsAt == null) {
                return "ON_AIR_OPEN_ENDED";
            }
            if (subscriptionEndsAt.isBefore(Instant.now())) {
                return "SUBSCRIPTION_ENDED";
            }
            return "ON_AIR";
        }
        return "APPROVED_OTHER";
    }
}
