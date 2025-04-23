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

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "clients")
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

    @Column(name = "password", columnDefinition = "TEXT", nullable = false)
    private String password;

    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role = Role.CLIENT;

    @Column(name = "business_field", nullable = false)
    private String businessField;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private DefaultStatus status = DefaultStatus.INACTIVE;

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
    @JoinColumn(name = "social_media_id", referencedColumnName = "id", nullable = true)
    private SocialMedia socialMedia;

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL)
    private List<Address> addresses = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL)
    private List<Attachment> attachments = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL)
    private List<AdvertisingAttachment> advertisingAttachments = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL)
    private List<Notification> notifications = new ArrayList<>();

    public Client(ClientRequestDto request) {
        businessName = request.getBusinessName();
        identificationNumber = request.getIdentificationNumber();
        businessField = request.getBusinessField();
        role = request.getRole();
        status = request.getStatus();
        contact = new Contact(request.getContact());
        owner = new Owner(request.getOwner(), this);
        socialMedia = request.getSocialMedia() != null ? new SocialMedia(request.getSocialMedia()) : null;
        addresses.addAll(request.getAddresses().stream().map(address -> new Address(address, this)).toList());
    }

    public void update(ClientRequestDto request) {
        businessName = request.getBusinessName();
        identificationNumber = request.getIdentificationNumber();
        businessField = request.getBusinessField();
        role = request.getRole();
        status = request.getStatus();
        contact.update(request.getContact());
        owner.update(request.getOwner());
        socialMedia.update(request.getSocialMedia());

        addresses.clear();
        addresses.addAll(request.getAddresses().stream().map(address -> new Address(address, this)).toList());
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
