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

  private final AdRequestClientResponseDto adRequest;

  private final Set<Address> addresses;

  private final List<LinkResponseDto> attachments;

  private final List<AdResponseDto> ads;

  private final boolean termAccepted;

  private final boolean hasSubscription;

  private final boolean shouldDisplayAttachments;

  private final int currentSubscriptionFlowStep;

  public ClientResponseDto(Client entity, List<LinkResponseDto> attachmentUrls, List<AdResponseDto> adsUrls) {
    id = entity.getId();
    businessName = entity.getBusinessName();
    identificationNumber = entity.getIdentificationNumber();
    role = entity.getRole();
    industry = entity.getIndustry();
    status = entity.getStatus();
    contact = entity.getContact();
    owner = entity.getOwner();
    socialMedia = entity.getSocialMedia();
    adRequest = entity.getAdRequest() != null ? new AdRequestClientResponseDto(entity.getAdRequest()) : null;
    addresses = entity.getAddresses();
    attachments = attachmentUrls;
    ads = adsUrls;
    termAccepted = entity.isTermsAccepted();
    currentSubscriptionFlowStep = entity.getSubscriptionFlow() != null ? entity.getSubscriptionFlow().getStep() : 0;
    hasSubscription = !entity.getSubscriptions().isEmpty();
    shouldDisplayAttachments = !entity.getAttachments().isEmpty() || !entity.getAds().isEmpty() || !entity.getSubscriptions().isEmpty();
  }
}
