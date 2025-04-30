package com.marketingproject.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.marketingproject.dtos.request.ClientRequestDto;
import com.marketingproject.enums.DefaultStatus;
import com.marketingproject.enums.Role;
import com.marketingproject.shared.audit.BaseAudit;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.NotAudited;

import java.io.Serial;
import java.io.Serializable;
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

    @Column(name = "business_name")
    private String businessName;

    @Column(name = "identification_number", nullable = false, unique = true)
    private String identificationNumber;

    @NotAudited
    @Column(name = "password", columnDefinition = "TEXT")
    private String password;

    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role = Role.CLIENT;

    @Column(name = "business_field", nullable = false)
    private String businessField;

    @Column(name = "status", columnDefinition = "default_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private DefaultStatus status = DefaultStatus.INACTIVE;

    @JsonIgnore
    @NotAudited
    @OneToOne
    @JoinColumn(name = "verification_code_id", referencedColumnName = "id", nullable = false)
    private VerificationCode verificationCode;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "contact_id", referencedColumnName = "id", nullable = false)
    private Contact contact;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "owner_id", referencedColumnName = "id", nullable = false)
    private Owner owner;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "social_media_id", referencedColumnName = "id")
    private SocialMedia socialMedia;

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<Address> addresses = new HashSet<>();

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL)
    private Set<Attachment> attachments = new HashSet<>();

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL)
    private Set<AdvertisingAttachment> advertisingAttachments = new HashSet<>();

    @NotAudited
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @OrderBy("createdAt DESC")
    private List<Notification> notifications = new ArrayList<>();

    public Client(ClientRequestDto request) {
        businessName = request.getBusinessName();
        identificationNumber = request.getIdentificationNumber();
        businessField = request.getBusinessField();
        role = request.getRole();
        status = request.getStatus();
        contact = new Contact(request.getContact());
        owner = new Owner(request.getOwner(), this);

        socialMedia = Optional.ofNullable(request.getSocialMedia())
                .map(SocialMedia::new)
                .orElse(null);

        addresses = request.getAddresses().stream()
                .map(address -> new Address(address, this))
                .peek(address -> address.setUsernameCreate(request.getBusinessName()))
                .collect(Collectors.toSet());

        setUsernameCreateForRelatedEntities(request.getBusinessName());
    }

    public void update(ClientRequestDto request, String updatedBy) {
        businessName = request.getBusinessName();
        identificationNumber = request.getIdentificationNumber();
        businessField = request.getBusinessField();
        role = request.getRole();
        status = request.getStatus();
        contact.update(request.getContact());
        owner.update(request.getOwner());
        Optional.ofNullable(socialMedia).ifPresent(socialMedia -> socialMedia.update(request.getSocialMedia()));

        addresses = request.getAddresses().stream()
                .map(address -> new Address(address, this))
                .peek(address -> address.setUsernameUpdate(updatedBy))
                .collect(Collectors.toSet());

        setUsernameUpdateForRelatedEntities(updatedBy);
    }

    private void setUsernameCreateForRelatedEntities(String username) {
        setUsernameCreate(username);
        contact.setUsernameCreate(username);
        owner.setUsernameCreate(username);
        if (socialMedia != null) {
            socialMedia.setUsernameCreate(username);
        }
    }

    private void setUsernameUpdateForRelatedEntities(String username) {
        setUsernameUpdate(username);
        contact.setUsernameUpdate(username);
        owner.setUsernameUpdate(username);
        if (socialMedia != null) {
            socialMedia.setUsernameUpdate(username);
        }
    }

    public boolean isAdmin() {
        return Role.ADMIN.equals(role);
    }

    public String toStringMapper() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        return objectMapper.writeValueAsString(this);
    }
}
