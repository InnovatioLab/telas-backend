package com.telas.enums;

import org.springframework.util.ObjectUtils;

import java.util.Map;

public enum NotificationReference {
  MONITOR_IN_WISHLIST_NOW_AVAILABLE {
    @Override
    public String getNotificationMessage(Map<String, String> params) {
      return String.format("""
                      <div class="informacoes">
                          <h4 id="notification-title" class="notification-title">Attachment Approved!</h4>
                          <div class="field">
                              <span id="attachment-name" class="field-label">%s</span>
                              <span class="field-value overflow-hidden white-space-nowrap"> has been approved.</span>
                          </div>
                      </div>
                      <a id="link-details" class='details link-text' href="%s">View Details</a>
                      """,
              params.get("attachmentName"), params.get("link"));
    }
  },
  AD_REFUSED {
    @Override
    public String getNotificationMessage(Map<String, String> params) {
      return String.format("""
                      <div class="informacoes">
                          <h4 id="notification-title" class="notification-title">Attachment Refused!</h4>
                          <div class="field">
                              <span id="attachment-name" class="field-label">%s</span>
                              <span class="field-value overflow-hidden white-space-nowrap"> has been rejected.</span>
                          </div>
                          <div class="campo">
                                <span id="justification" class="field-label">Justification:</span>
                                <span id="justification-value" class="field-value text-overflow-ellipsis overflow-hidden white-space-nowrap">%s</span>
                            </div>
                            <div class="campo">
                                <span id="description" class="field-label">Description:</span>
                                <span id="description-value" class="field-value text-overflow-ellipsis overflow-hidden white-space-nowrap text-overflow-ellipsis">%s</span>
                            </div>
                      </div>
                      <a id="link-details" class='details link-text' href="%s">View Details</a>
                      """,
              params.get("attachmentName"),
              params.get("justification"),
              !ObjectUtils.isEmpty(params.get("description")) && params.get("description") != null ? params.get("description") : "",
              params.get("link"));
    }
  };

  public abstract String getNotificationMessage(Map<String, String> params);
}
