package com.telas.shared.constants.valitation;

public final class AdValidationMessages {
  public static final String AD_NOT_FOUND = "Ad not found";
  public static final String PENDING_VALIDATION_NOT_ACCEPTED = "Pending validation not accepted";
  public static final String ATTACHMENT_IDS_REQUIRED = "Attachment IDs are required";
  public static final String MESSAGE_REQUIRED = "Message is required";
  public static final String AD_REQUEST_ID_REQUIRED = "Ad request ID is required";
  public static final String AD_REQUEST_NOT_FOUND = "Ad request not found";
  public static final String AD_EXCEEDS_MAX_VALIDATION = "Ad exceeds max validation, please contact support";
  public static final String FILE_NAME_MUST_BE_CHANGED_DURING_UPDATE = "File name must be changed during update";
  public static final String ADMIN_ROLE_REQUIRED = "Admin role is required for this operation";
  public static final String VALIDATION_NOT_ALLOWED = "You are not allowed to validate this ad";
  public static final String AD_ALREADY_VALIDATED = "Ad already validated";
  public static final String AD_MUST_BE_APPROVED_TO_DELETE = "Only client-approved ads can be deleted from this screen";
  public static final String AD_NOT_ELIGIBLE_FOR_DELETE = "This ad cannot be deleted in its current state";
  public static final String AD_MUST_BE_PARTNER_ADVERTISER_TO_DELETE =
          "Only partner advertiser ads can be deleted from this section";
  public static final String AD_MUST_BE_APPROVED_FOR_BOX_DISPATCH =
          "Only approved ads can be sent to the box";
  public static final String AD_NOT_PLACED_ON_MONITOR =
          "Ad must be placed on a monitor before sending to the box";
  public static final String BOX_DISPATCH_NOT_AVAILABLE =
          "Could not sync to the box: monitor has no active box with IP, or the box did not accept the playlist";
  public static final String BOX_DISPATCH_STAGE_NOT_AVAILABLE =
          "Could not stage the ad on the box: monitor has no active box with IP, or the box did not accept the file";
  public static final String AD_MONITOR_TARGET_NOT_FOUND =
          "No target screen found for this ad";
  public static final String AD_MUST_BE_APPROVED_FOR_PARTNER_REVIEW_DELIVERY =
          "Only approved or partner-rejected ads can be replaced for partner review";
  public static final String AD_NOT_PARTNER_ADVERTISER =
          "This action applies to partner ads only";
  public static final String AD_REMOVAL_NOT_ALLOWED =
          "This ad cannot be removed yet — it must be on a screen playlist first";
  public static final String AD_NOT_OWNED_BY_PARTNER =
          "You can only request removal for your own ads";
  public static final String AD_REMOVAL_ALREADY_REQUESTED =
          "A removal request for this ad was already submitted";
}
