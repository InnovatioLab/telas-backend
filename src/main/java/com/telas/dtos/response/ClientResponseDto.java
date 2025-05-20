package com.telas.dtos.response;

import com.telas.entities.*;
import com.telas.enums.DefaultStatus;
import com.telas.enums.Role;
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

  private final String industry;

  private final DefaultStatus status;

  private final Contact contact;

  private final Owner owner;

  private final SocialMedia socialMedia;

  private final Set<Address> addresses;

  private final List<LinkResponseDto> attachments;

  private final List<LinkResponseDto> ads;

  private final List<Notification> notifications;

  private final Boolean termAccepted;

  private final int currentSubscriptionFlowStep;

  public ClientResponseDto(Client entity, List<LinkResponseDto> attachmentUrls, List<LinkResponseDto> adsUrls) {
    id = entity.getId();
    businessName = entity.getBusinessName();
    identificationNumber = entity.getIdentificationNumber();
    role = entity.getRole();
    industry = entity.getIndustry();
    status = entity.getStatus();
    contact = entity.getContact();
    owner = entity.getOwner();
    socialMedia = entity.getSocialMedia();
    addresses = entity.getAddresses();
    attachments = attachmentUrls;
    ads = adsUrls;
    notifications = entity.getNotifications();
    termAccepted = entity.isTermsAccepted();
    currentSubscriptionFlowStep = entity.getSubscriptionFlow().getStep();
  }
}
