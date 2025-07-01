package com.telas.shared.constants;

public class MessageCommonsConstants {
  public static final String SAVE_SUCCESS_MESSAGE = "The record has been successfully saved!";
  public static final String UPDATE_SUCCESS_MESSAGE = "The record has been successfully updated!";
  public static final String CODE_SENT_SUCCESS_MESSAGE = "A new code has been sent to your main contact! Please check and try again.";
  public static final String PASSWORD_CHANGED_SUCCESS_MESSAGE = "Password successfully changed! You can access the platform with your new password.";
  public static final String PASSWORD_RESET_SUCCESS_MESSAGE = "Password successfully reset! You can access the platform with your new password.";
  public static final String CODE_CONFIRMED_SUCCESS_MESSAGE = "Code successfully confirmed!";
  public static final String UPLOAD_SUCCESS_MESSAGE = "The files have been successfully saved!";
  public static final String LOGIN_SUCCESS_MESSAGE = "Login successfully completed.";
  public static final String FIND_ID_SUCCESS_MESSAGE = "Record found.";
  public static final String FIND_ALL_SUCCESS_MESSAGE = "Records found.";
  public static final String FIND_FILTER_EMPTY_MESSAGE = "No results found! Try changing the filters or the search term.";
  public static final String START_PAGE_ERROR_MESSAGE = "Pagination must start from page 1.";
  public static final String ACCEPT_TERMS_CONDITIONS_SUCCESS_MESSAGE = "Terms and conditions accepted successfully!";
  public static final String AD_VALIDATION_MESSAGE = "Ad successfully validated!";
  public static final String REQUEST_AD_SUCCESS_MESSAGE = "Ad creation request successfully sent!";
  public static final String SUBSCRIPTION_CANCELLED_SUCCESS_MESSAGE = "Subscription successfully cancelled!";
  public static final String DELETE_SUCCESS_MESSAGE = "The record has been successfully deleted!";
  public static final String NOTIFICATION_NOT_FOUND = "Notification not found!";


  // Private constructor to prevent instantiation

  private MessageCommonsConstants() {
  }

  public static String getSaveSuccessMessage(String name) {
    return getSaveSuccessMessage(name, false);
  }

  public static String getSaveSuccessMessage(String name, boolean isRegistration) {
    boolean endsWithA = name.endsWith("a") || name.endsWith("A");
    String preposition;

    if (endsWithA) {
      preposition = isRegistration ? "registered " : "created ";
    } else {
      preposition = isRegistration ? "registered " : "created ";
    }

    return name + " " + preposition + "successfully" + (isRegistration ? "!" : ".");
  }

  public static String getUpdateSuccessMessage(String name) {
    String preposition = (name.endsWith("a") || name.endsWith("A")) ? "updated " : "updated ";
    return name + " " + preposition + "successfully.";
  }

  public static String getFilterQuerySuccessMessage(String name) {
    String preposition = (name.endsWith("a") || name.endsWith("A")) ? "found." : "found.";
    return name + "s " + preposition;
  }
}
