package com.telas.enums;

import com.telas.dtos.EmailDataDto;
import com.telas.shared.constants.SharedConstants;
import org.springframework.util.ObjectUtils;

import java.util.Map;

public enum NotificationReference {
  FIRST_SUBSCRIPTION {
    @Override
    public String getNotificationMessage(Map<String, String> params) {
      return String.format("""
                      <div class="informacoes">
                          <h4 id="notification-title" class="notification-title">Thank you for your recent purchase!</h4>
                          <div class="field">
                              <span class="field-value overflow-hidden white-space-nowrap"> Now you're able to upload attachments and send a request to ad creation.</span>
                          </div>
                      </div>
                      <a id="link-details" class='details link-text' href="%s">Upload your attachments</a>
                      """,
              params.get("link"));
    }

    @Override
    public EmailDataDto getEmailData(Map<String, String> params) {
      EmailDataDto emailData = new EmailDataDto();
      emailData.setSubject(SharedConstants.EMAIL_SUBJECT_FIRST_SUBSCRIPTION);
      emailData.setTemplate(SharedConstants.TEMPLATE_EMAIL_FIRST_SUBSCRIPTION);
      emailData.getParams().put("name", params.get("name"));
      emailData.getParams().put("attachmentId", params.get("attachmentId"));
      emailData.getParams().put("locations", params.get("locations"));
      emailData.getParams().put("startDate", "startDate");
      emailData.getParams().put("endDate", ObjectUtils.isEmpty(params.get("endDate")) ? "" : params.get("endDate"));
      return emailData;
    }
  },
  SUBSCRIPTION_ABOUT_TO_EXPIRY {
    @Override
    public String getNotificationMessage(Map<String, String> params) {
      return String.format("""
                      <div class="info">
                          <h4 id="notification-title" class="notification-title">Your Subscription is about to expiry!</h4>
                          <p><We hope you’ve enjoyed your Ad service with Telas. We wanted to  remind you that your current service is set to expire soon.</p>
                          <div class="field">
                              <span id="attachment-name" class="field-label">Service End Date: </span>
                              <span class="field-value overflow-hidden white-space-nowrap">%s</span>
                          </div>
                      </div>
                      <p>To continue enjoying our services without interruption, please visit this <a id="link-details" class='details link-text' href="%s">link</a> and renew your subscription before the end date.</p>
                      """,
              params.get("endDate"), params.get("link"));
    }

    @Override
    public EmailDataDto getEmailData(Map<String, String> params) {
      EmailDataDto emailData = new EmailDataDto();
      emailData.setSubject(SharedConstants.EMAIL_SUBJECT_SUBSCRIPTION_EXPIRING);
      emailData.setTemplate(SharedConstants.TEMPLATE_EMAIL_SUBSCRIPTION_EXPIRING);
      emailData.getParams().put("name", params.get("name"));
      emailData.getParams().put("link", params.get("link"));
      emailData.getParams().put("endDate", params.get("endDate"));
      return emailData;
    }
  },
  AD_RECEIVED {
    @Override
    public String getNotificationMessage(Map<String, String> params) {
      return String.format("""
                      <div class="info">
                          <h4 id="notification-title" class="notification-title">You received a new Ad!</h4>
                          <p> Please visit this <a id="link-details" class='details link-text' href="%s">link</a> to validate it and and start to use your service!</p>
                      </div>
                      """,
              params.get("link"));
    }

    @Override
    public EmailDataDto getEmailData(Map<String, String> params) {
      return null;
    }
  },
  MONITOR_IN_WISHLIST_NOW_AVAILABLE {
    @Override
    public String getNotificationMessage(Map<String, String> params) {
      return String.format("""
                      <div class="info">
                          <h4 id="notification-title" class="notification-title">A monitor in your wishlist is now Available!</h4>
                      </div>
                      <a id="link-details" class='details link-text' href="%s">View Details</a>
                      """,
              params.get("link"));
    }

    @Override
    public EmailDataDto getEmailData(Map<String, String> params) {
      EmailDataDto emailData = new EmailDataDto();
      emailData.setSubject("");
      emailData.setTemplate("");
      emailData.getParams().put("attachmentName", params.get("attachmentName"));
      emailData.getParams().put("attachmentId", params.get("attachmentId"));
      emailData.getParams().put("link", params.get("link"));
      emailData.getParams().put("description", ObjectUtils.isEmpty(params.get("description")) ? "" : params.get("description"));
      return emailData;
    }
  };

  public abstract String getNotificationMessage(Map<String, String> params);

  public abstract EmailDataDto getEmailData(Map<String, String> params);
}
