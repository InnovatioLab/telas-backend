package com.telas.shared.constants.valitation;

public final class ClientValidationMessages {
  public static final String BUSINESS_NAME_REQUIRED = "Business name is required.";
  public static final String BUSINESS_NAME_SIZE = "Business name must be lass than 255 characters.";
  public static final String INDUSTRY_REQUIRED = "Industry is required.";
  public static final String INDUSTRY_SIZE = "Industry must be less than 50 characters.";
  public static final String CONTACT_REQUIRED = "Contact is required.";
  public static final String OWNER_REQUIRED = "Owner is required.";
  public static final String ADDRESSES_REQUIRED = "Addresses are required.";
  public static final String INDUSTRY_REGEX = "Industry must contain only letters.";
  public static final String INVALID_OR_EXPIRED_CODE = "Invalid or expired code.";
  public static final String IDENTIFICATION_NUMBER_UNIQUE = "Client identification number must be unique.";
  public static final String EMAIL_UNIQUE = "Client email must be unique.";
  public static final String USER_NOT_FOUND = "User not found.";
  public static final String PASSWORD_REQUIRED = "Password is required.";
  public static final String PASSWORD_REGEX = "Password must contain at least one letter, one number, and one special character.";
  public static final String PASSWORD_SIZE = "Password must be between 8 and 32 characters.";
  public static final String CONFIRM_PASSWORD_REQUIRED = "Confirm password is required.";
  public static final String DIFFERENT_PASSWORDS = "Passwords do not match.";
  public static final String VALIDATION_CODE_NOT_VALIDATED = "User validation code is not validated.";
  public static final String CURRENT_EQUALS_NEW_PASSWORD = "Current password cannot be the same as the new password.";
  public static final String SOCIAL_MEDIA_REQUIRED = "At least one social media URL must be provided.";
  public static final String AD_REQUEST_NOT_ACTIVE = "Ad request is not active.";
  public static final String MAX_ADS_REACHED = "Client has reached the maximum number of ads allowed.";
  public static final String MONITOR_IN_ACTIVE_SUBSCRIPTION = "Client has an active subscription for the selected monitor.";
  public static final String WISHLIST_NOT_FOUND = "Wishlist not found!";
  public static final String OWNER_EMAIL_UNIQUE = "Owner email must be unique.";
  public static final String MONITORS_WITH_ACTIVE_SUBSCRIPTIONS_NOT_FOUND = "Monitors with active subscriptions not found.";
  public static final String UNAUTHORIZED_CLIENT = "You are not authorized to perform this action.";
  public static final String AD_REQUEST_EXISTS = "Ad request already exists for this client, please wait for the current request to be processed.";
  public static final String AD_REQUEST_NOT_ALLOWED = "Ad request is not allowed for this client.";
}
