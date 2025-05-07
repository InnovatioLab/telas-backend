package com.telas.enums;

import com.telas.dtos.EmailDataDto;
import org.springframework.util.ObjectUtils;

import java.util.Map;

public enum NotificationReference {
    ATTACHMENT_APPROVED {
        @Override
        public String getPhoneMessage(Map<String, String> params) {
            return String.format("""
                            Your attachment with name: %s has been approved. See details: %s
                            """,
                    params.get("attachmentName"),
                    params.get("link"));
        }

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

        @Override
        public EmailDataDto getEmailData(Map<String, String> params) {
            String subject = "Attachment Approved!";
            String template = "attachment_validation.ftlh";
            EmailDataDto emailData = new EmailDataDto();
            emailData.setSubject(subject);
            emailData.setTemplate(template);
            emailData.getParams().put("attachmentName", params.get("attachmentName"));
            emailData.getParams().put("attachmentId", params.get("attachmentId"));
            emailData.getParams().put("status", "Approved");
            emailData.getParams().put("recipient", params.get("recipient"));
            emailData.getParams().put("link", params.get("link"));
            return emailData;
        }
    },
    ATTACHMENT_REFUSED {
        @Override
        public String getPhoneMessage(Map<String, String> params) {
            return String.format("""
                            Your attachment with name: %s has been refused. Justification: %s. Description: %s. See details: %s
                            """,
                    params.get("attachmentName"),
                    params.get("justification"),
                    !ObjectUtils.isEmpty(params.get("description")) && params.get("description") != null ? params.get("description") : "",
                    params.get("link"));
        }

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

        @Override
        public EmailDataDto getEmailData(Map<String, String> params) {
            String subject = "Attachment Refused!";
            String template = "attachment_validation.ftlh";
            EmailDataDto emailData = new EmailDataDto();
            emailData.setSubject(subject);
            emailData.setTemplate(template);
            emailData.getParams().put("attachmentName", params.get("attachmentName"));
            emailData.getParams().put("attachmentId", params.get("attachmentId"));
            emailData.getParams().put("recipient", params.get("recipient"));
            emailData.getParams().put("status", "Refused");
            emailData.getParams().put("link", params.get("link"));
            emailData.getParams().put("justification", params.get("justification"));
            emailData.getParams().put("description", ObjectUtils.isEmpty(params.get("description")) ? "" : params.get("description"));
            return emailData;
        }
    };


    public abstract String getPhoneMessage(Map<String, String> params);

    public abstract String getNotificationMessage(Map<String, String> params);

    public abstract EmailDataDto getEmailData(Map<String, String> params);
}
