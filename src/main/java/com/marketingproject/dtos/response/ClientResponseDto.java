package com.marketingproject.dtos.response;

import com.marketingproject.entities.*;
import com.marketingproject.enums.DefaultStatus;
import com.marketingproject.enums.Role;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
public final class ClientResponseDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 5288515525105234502L;

    private final UUID id;

    private final String businessName;

    private final String identificationNumber;

    private final Role role;

    private final String businessField;

    private final DefaultStatus status;

    private final Contact contact;

    private final Owner owner;

    private final SocialMedia socialMedia;

    private final Set<Address> addresses;

    private final Set<Attachment> attachments;

    private final Set<AdvertisingAttachment> advertisingAttachments;

    private final List<Notification> notifications;


    public ClientResponseDto(Client entity) {
        id = entity.getId();
        businessName = entity.getBusinessName();
        identificationNumber = entity.getIdentificationNumber();
        role = entity.getRole();
        businessField = entity.getBusinessField();
        status = entity.getStatus();
        contact = entity.getContact();
        owner = entity.getOwner();
        socialMedia = entity.getSocialMedia();
        addresses = entity.getAddresses();
        attachments = entity.getAttachments();
        advertisingAttachments = entity.getAdvertisingAttachments();
        notifications = entity.getNotifications();
    }
}
