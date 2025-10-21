package com.telas.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.telas.dtos.request.MonitorRequestDto;
import com.telas.enums.MonitorType;
import com.telas.enums.Recurrence;
import com.telas.enums.SubscriptionStatus;
import com.telas.shared.audit.BaseAudit;
import com.telas.shared.constants.SharedConstants;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.NotAudited;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
@Entity
@Table(name = "monitors")
@AuditTable("monitors_aud")
@NoArgsConstructor
public class Monitor extends BaseAudit implements Serializable {
    @Serial
    private static final long serialVersionUID = 1084934057135367842L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @Column(name = "fl_active", nullable = false)
    private boolean active = true;

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private MonitorType type = MonitorType.BASIC;

    @Column(name = "location_description")
    private String locationDescription;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "max_blocks")
    private Integer maxBlocks = SharedConstants.MAX_MONITOR_ADS;

    @ManyToOne
    @JoinColumn(name = "address_id", referencedColumnName = "id", nullable = false)
    private Address address;

    @JsonIgnore
    @OneToMany(mappedBy = "id.monitor", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MonitorAd> monitorAds = new HashSet<>();

    @JsonIgnore
    @ManyToMany(mappedBy = "monitors")
    private Set<Subscription> subscriptions;

    @JsonIgnore
    @NotAudited
    @ManyToOne
    @JoinColumn(name = "box_id", referencedColumnName = "id")
    private Box box;

    public Monitor(MonitorRequestDto request, Address address, String productId) {
        this.productId = productId;
        type = request.getType();
        locationDescription = request.getLocationDescription();
        this.address = address;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Monitor monitor = (Monitor) o;
        return Objects.equals(getId(), monitor.getId());
    }

    public List<Ad> getAds() {
        return monitorAds.stream()
                .map(MonitorAd::getAd)
                .toList();
    }

    public boolean hasAvailableBlocks(int blocksWanted) {
        return isWithinAdsLimit(blocksWanted) && isWithinSubscriptionsLimit(blocksWanted);
    }

    public boolean isWithinAdsLimit(int blocksWanted) {
        return monitorAds.size() + blocksWanted <= maxBlocks;
    }

    private boolean isWithinSubscriptionsLimit(int blocksWanted) {
        return getActiveSubscriptions().size() + blocksWanted <= maxBlocks;
    }

    public Set<Subscription> getActiveSubscriptions() {
        Instant now = Instant.now();
        return subscriptions.stream()
                .filter(subscription -> SubscriptionStatus.ACTIVE.equals(subscription.getStatus())
                        && (subscription.getEndsAt() == null || subscription.getEndsAt().isAfter(now)))
                .collect(Collectors.toSet());
    }

    public boolean clientAlreadyHasAd(Client client) {
        return getAds().stream()
                .anyMatch(ad -> ad.getClient() != null && ad.getClient().getId().equals(client.getId()));
    }

    public boolean isAbleToSendBoxRequest() {
        return box != null && box.isActive();
    }

    public Instant getEstimatedSlotReleaseDate() {
        return getActiveSubscriptions().stream()
                .filter(subscription -> !Recurrence.MONTHLY.equals(subscription.getRecurrence()))
                .map(Subscription::getEndsAt)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);
    }

    public Integer getAdsDailyDisplayTimeInMinutes() {
        LocalTime localTime = LocalTime.ofInstant(Instant.now(), java.time.ZoneId.of(SharedConstants.ZONE_ID));

        if (monitorAds.isEmpty()) {
            return 0;
        }

        int adsCount = monitorAds.size();
        int secondsOfDay = localTime.toSecondOfDay();
        int loopDuration = adsCount * SharedConstants.AD_DISPLAY_TIME_IN_SECONDS;
        int loops = secondsOfDay / loopDuration;
        int totalSeconds = loops * SharedConstants.AD_DISPLAY_TIME_IN_SECONDS;
        return totalSeconds / SharedConstants.TOTAL_SECONDS_IN_A_MINUTE;
    }
}
