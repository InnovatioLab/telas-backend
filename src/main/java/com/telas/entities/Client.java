package com.telas.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.telas.dtos.request.ClientRequestDto;
import com.telas.enums.AdValidationType;
import com.telas.enums.DefaultStatus;
import com.telas.enums.Role;
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
import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
@Entity
@Table(name = "clients")
@AuditTable("clients_aud")
@NoArgsConstructor
public class Client extends BaseAudit implements Serializable {
    @Serial
    private static final long serialVersionUID = 1084934057135367842L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotAudited
    @Column(name = "stripe_customer_id")
    private String stripeCustomerId;

    @Column(name = "business_name")
    private String businessName;

    @NotAudited
    @Column(name = "password", columnDefinition = "TEXT")
    private String password;

    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role = Role.CLIENT;

    @Column(name = "industry")
    private String industry;

    @Column(name = "website_url", columnDefinition = "TEXT")
    private String websiteUrl;

    @Column(name = "status", columnDefinition = "default_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private DefaultStatus status = DefaultStatus.INACTIVE;

    @NotAudited
    @Column(name = "term_accepted_at", columnDefinition = "TIMESTAMP WITHOUT TIME ZONE")
    private Instant termAcceptedAt;

    @NotAudited
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "term_condition_id", referencedColumnName = "id")
    private TermCondition termCondition;

    @JsonIgnore
    @NotAudited
    @OneToOne
    @JoinColumn(name = "verification_code_id", referencedColumnName = "id", nullable = false)
    private VerificationCode verificationCode;

    @NotAudited
    @OneToOne(mappedBy = "client", cascade = CascadeType.ALL)
    private SubscriptionFlow subscriptionFlow;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "contact_id", referencedColumnName = "id", nullable = false)
    private Contact contact;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "social_media_id", referencedColumnName = "id")
    private SocialMedia socialMedia;

    @NotAudited
    @JsonIgnore
    @OneToOne(mappedBy = "client", cascade = CascadeType.ALL)
    private AdRequest adRequest;

    @NotAudited
    @OneToOne(mappedBy = "client", cascade = CascadeType.ALL)
    private Wishlist wishlist;

    @JsonIgnore
    @OneToMany(mappedBy = "client")
    private List<Subscription> subscriptions = new ArrayList<>();

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL)
    private List<Address> addresses = new ArrayList<>();

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL)
    private List<Attachment> attachments = new ArrayList<>();

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL)
    private List<Ad> ads = new ArrayList<>();

    @NotAudited
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL)
    private List<Notification> notifications = new ArrayList<>();

    public Client(ClientRequestDto request) {
        businessName = request.getBusinessName();
        industry = request.getIndustry();
        websiteUrl = request.getWebsiteUrl();
        status = request.getStatus();
        contact = new Contact(request.getContact());
        wishlist = new Wishlist(this);

        socialMedia = Optional.ofNullable(request.getSocialMedia())
                .map(SocialMedia::new)
                .orElse(null);

        addresses = request.getAddresses().stream()
                .map(address -> new Address(address, this))
                .collect(Collectors.toList());

        setUsernameCreateForRelatedEntities(request.getBusinessName());
    }

    public void update(ClientRequestDto request, String updatedBy) {
        industry = request.getIndustry();
        websiteUrl = request.getWebsiteUrl();
        contact.update(request.getContact());

        if (request.getSocialMedia() != null) {
            if (socialMedia == null) {
                socialMedia = new SocialMedia(request.getSocialMedia());
            } else {
                socialMedia.update(request.getSocialMedia());
            }
        } else {
            socialMedia = null;
        }

        setUsernameUpdateForRelatedEntities(updatedBy);
    }

    public List<Ad> getApprovedAds() {
            return ads.stream()
                    .filter(ad -> AdValidationType.APPROVED.equals(ad.getValidation()))
                    .toList();
    }

    public boolean isFirstSubscription() {
        long nonBonusSubscriptions = subscriptions.stream()
                .filter(subs -> !subs.isBonus())
                .count();
        return nonBonusSubscriptions == SharedConstants.MIN_QUANTITY_MONITOR_BLOCK;
    }


    private void setUsernameCreateForRelatedEntities(String username) {
        setUsernameCreate(username);
        contact.setUsernameCreate(username);
        Optional.ofNullable(socialMedia).ifPresent(sm -> sm.setUsernameCreate(username));
    }

    private void setUsernameUpdateForRelatedEntities(String username) {
        setUsernameUpdate(username);
        contact.setUsernameUpdate(username);
        Optional.ofNullable(socialMedia).ifPresent(sm -> sm.setUsernameUpdate(username));
    }

    public boolean isAdmin() {
        return Role.ADMIN.equals(role);
    }

    public boolean hasActiveSubscription() {
        if (isAdmin()) {
            return true;
        }

        return subscriptions.stream().anyMatch(subscription -> SubscriptionStatus.ACTIVE.equals(subscription.getStatus()));
    }

    public boolean isTermsAccepted() {
        return termCondition != null && termAcceptedAt != null;
    }

    public boolean isPartner() {
        return Role.PARTNER.equals(role);
    }
}
