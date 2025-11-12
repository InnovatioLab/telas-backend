package com.telas.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.telas.dtos.request.MonitorRequestDto;
import com.telas.enums.Recurrence;
import com.telas.enums.SubscriptionStatus;
import com.telas.infra.exceptions.BusinessRuleException;
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

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "max_blocks")
    private Integer maxBlocks = SharedConstants.MAX_MONITOR_ADS;

    @OneToOne
    @JoinColumn(name = "address_id", unique = true, nullable = false)
    private Address address;

    @JsonIgnore
    @OneToMany(mappedBy = "id.monitor", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MonitorAd> monitorAds = new HashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "id.monitor")
    private Set<SubscriptionMonitor> subscriptionMonitors = new HashSet<>();
    
    @JsonIgnore
    @Transient
    public Set<Subscription> getSubscriptions() {
        return subscriptionMonitors.stream()
                .map(SubscriptionMonitor::getSubscription)
                .collect(Collectors.toSet());
    }

    @JsonIgnore
    @NotAudited
    @ManyToOne
    @JoinColumn(name = "box_id", referencedColumnName = "id")
    private Box box;

    public Monitor(Address address, String productId) {
        if (address == null || !address.getClient().isPartner()) {
            throw new BusinessRuleException("Partner client invalid");
        }
        this.productId = productId;
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

    public boolean hasAvailableBlocks(CartItem item) {
        return isWithinAdsLimit(item) && isWithinSubscriptionsLimit(item);
    }

    public boolean isWithinAdsLimit(CartItem item) {
        // Se o monitor tem um partner, reserva 7 slots para ads do partner
        int partnerAdsSlots = getPartnerAdsSlots();
        int otherAdsSize = monitorAds.size() - partnerAdsSlots;
        
        // Slots disponíveis para outros clientes = maxBlocks - slots reservados para partner
        int availableSlotsForOthers = maxBlocks - SharedConstants.PARTNER_RESERVED_SLOTS;
        return otherAdsSize + item.getBlockQuantity() <= availableSlotsForOthers;
    }

    public boolean isWithinAdsLimit(int blocksWanted) {
        int partnerAdsSlots = getPartnerAdsSlots();
        int otherAdsSize = monitorAds.size() - partnerAdsSlots;
        int availableSlotsForOthers = maxBlocks - SharedConstants.PARTNER_RESERVED_SLOTS;
        return otherAdsSize + blocksWanted <= availableSlotsForOthers;
    }

    private boolean isWithinSubscriptionsLimit(CartItem item) {
        // Se o monitor tem um partner, sempre reserva 7 slots para ele
        int totalActiveSubscriptionSlots = getTotalActiveSubscriptionSlots(); // Todos os slots ativos
        int otherClientsSubscriptionSlots = getOtherClientsSubscriptionSlots(totalActiveSubscriptionSlots); // Slots ocupados pelo partner
        int availableSlotsForOthers = maxBlocks - SharedConstants.PARTNER_RESERVED_SLOTS;
        
        return otherClientsSubscriptionSlots + item.getBlockQuantity() <= availableSlotsForOthers;
    }

    private int getOtherClientsSubscriptionSlots(int totalActiveSubscriptionSlots) {
        return totalActiveSubscriptionSlots - getPartnerSubscriptionSlots();
    }

    private int getPartnerAdsSlots() {
        // Partner tem até 7 slots reservados para ads
        return monitorAds.stream()
                .filter(monitorAd -> {
                    Client adClient = monitorAd.getAd().getClient();
                    return adClient.isPartner() && adClient.getId().equals(getPartner().getId());
                })
                .mapToInt(ma -> 1)
                .sum();
    }

    private int getPartnerSubscriptionSlots() {
        return getActiveSubscriptionMonitors().stream()
                .filter(sm -> sm.getSubscription().getClient().isPartner()
                        && sm.getSubscription().getClient().getId().equals(getPartner().getId()))
                .mapToInt(SubscriptionMonitor::getSlotsQuantity)
                .sum();
    }

    private int getTotalActiveSubscriptionSlots() {
        return getActiveSubscriptionMonitors().stream()
                .mapToInt(SubscriptionMonitor::getSlotsQuantity)
                .sum();
    }


    public Set<SubscriptionMonitor> getActiveSubscriptionMonitors() {
        Instant now = Instant.now();
        return subscriptionMonitors.stream()
                .filter(sm -> {
                    Subscription subscription = sm.getSubscription();
                    return SubscriptionStatus.ACTIVE.equals(subscription.getStatus())
                            && (subscription.getEndsAt() == null || subscription.getEndsAt().isAfter(now));
                })
                .collect(Collectors.toSet());
    }

    public Set<Subscription> getActiveSubscriptions() {
        return getActiveSubscriptionMonitors().stream()
                .map(SubscriptionMonitor::getSubscription)
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
        return getActiveSubscriptionMonitors().stream()
                .map(SubscriptionMonitor::getSubscription)
                .filter(sub -> !Recurrence.MONTHLY.equals(sub.getRecurrence()))
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

    public boolean isPartner(Client client) {
        return client.isPartner() && address.getClient().getId().equals(client.getId());
    }

    private Client getPartner() {
        return address.getClient();
    }
}
