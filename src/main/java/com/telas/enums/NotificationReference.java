package com.telas.enums;

import com.telas.dtos.EmailDataDto;
import com.telas.shared.constants.SharedConstants;
import org.springframework.util.ObjectUtils;

import java.util.Map;

public enum NotificationReference {
    FIRST_SUBSCRIPTION {
        @Override
        public String getNotificationMessage(Map<String, String> params) {
            return formatNotificationMessage(
                    "Subscription Confirmed!",
                    "Your plan is now active.",
                    params,
                    createEndDateDiv(params.get("endDate")),
                    "Upload your attachments"
            );
        }

        @Override
        public EmailDataDto getEmailData(Map<String, String> params) {
            return createEmailData(
                    SharedConstants.EMAIL_SUBJECT_FIRST_SUBSCRIPTION,
                    SharedConstants.TEMPLATE_EMAIL_FIRST_SUBSCRIPTION,
                    params,
                    params.get("startDate"),
                    params.get("endDate")
            );
        }
    },
    NEW_SUBSCRIPTION {
        @Override
        public String getNotificationMessage(Map<String, String> params) {
            return formatNotificationMessage(
                    "Subscription Confirmed!",
                    "Your plan is now active.",
                    params,
                    createEndDateDiv(params.get("endDate")),
                    "Manage yours subscriptions"
            );
        }

        @Override
        public EmailDataDto getEmailData(Map<String, String> params) {
            return null;
        }
    },
    SUBSCRIPTION_RENEWAL {
        @Override
        public String getNotificationMessage(Map<String, String> params) {
            return formatNotificationMessage(
                    "Subscription Successfully Renewed!",
                    "Thank you for renewing your subscription.",
                    params,
                    createEndDateDiv(params.get("endDate"), "New End Date"),
                    "Manage yours subscriptions"
            );
        }

        @Override
        public EmailDataDto getEmailData(Map<String, String> params) {
            return null;
        }
    },
    SUBSCRIPTION_UPGRADE {
        @Override
        public String getNotificationMessage(Map<String, String> params) {
            return formatNotificationMessage(
                    "Subscription Upgraded Successfully!",
                    "Thank you for upgrading your subscription.",
                    params,
                    createEndDateDiv(params.get("endDate"), "New End Date"),
                    "Manage yours subscriptions"
            );
        }

        @Override
        public EmailDataDto getEmailData(Map<String, String> params) {
            return null;
        }
    },
    SUBSCRIPTION_ABOUT_TO_EXPIRY_REMINDER {
        @Override
        public String getNotificationMessage(Map<String, String> params) {
            return String.format("""
                    <div class="info">
                        <h4 id="notification-title" class="notification-title">15 Days Before Expiration</h4>
                        <p>We hope you’ve enjoyed your Ad service with Telas. We wanted to remind you that your current service is set to expire soon.</p>
                        <div class="field">
                            <span id="attachment-name" class="field-label">Service End Date: </span>
                            <span class="field-value overflow-hidden white-space-nowrap">%s</span>
                        </div>
                    </div>
                    <p>To continue enjoying our services without interruption, please visit this <a id="link-details" class='details link-text' href="%s">link</a> and renew your subscription before the end date.</p>
                    """, params.get("endDate"), params.get("link"));
        }

        @Override
        public EmailDataDto getEmailData(Map<String, String> params) {
            return createEmailData(
                    SharedConstants.EMAIL_SUBJECT_SUBSCRIPTION_EXPIRING_REMINDER,
                    SharedConstants.TEMPLATE_EMAIL_SUBSCRIPTION_EXPIRING_REMINDER,
                    params,
                    null,
                    params.get("endDate")
            );
        }
    },
    SUBSCRIPTION_ABOUT_TO_EXPIRY_LAST_DAY {
        @Override
        public String getNotificationMessage(Map<String, String> params) {
            return String.format("""
                    <div class="info">
                        <h4 id="notification-title" class="notification-title">Last Day of Service!</h4>
                        <p>Today marks the last day of your Telas advertising service.</p>
                    </div>
                    <p>Don’t let your momentum fade, please visit this <a id="link-details" class='details link-text' href="%s">link</a> and renew your subscription now.</p>
                    """, params.get("link"));
        }

        @Override
        public EmailDataDto getEmailData(Map<String, String> params) {
            return createEmailData(
                    SharedConstants.EMAIL_SUBJECT_SUBSCRIPTION_EXPIRING_LAST_DAY,
                    SharedConstants.TEMPLATE_EMAIL_SUBSCRIPTION_EXPIRING_LAST_DAY,
                    params,
                    null,
                    null
            );
        }
    },
    AD_RECEIVED {
        @Override
        public String getNotificationMessage(Map<String, String> params) {
            return String.format("""
                    <div class="info">
                        <h4 id="notification-title" class="notification-title">You received a new Ad!</h4>
                        <p>Please visit this <a id="link-details" class='details link-text' href="%s">link</a> to validate it and start to use your service!</p>
                    </div>
                    """, params.get("link"));
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
                    """, params.get("link"));
        }

        @Override
        public EmailDataDto getEmailData(Map<String, String> params) {
            return null;
        }
    },
    AD_NOT_SENT_TO_MONITOR {
        @Override
        public String getNotificationMessage(Map<String, String> params) {
            return String.format("""
                            <div class="info">
                                <h4 id="notification-title" class="notification-title">An approved ad cannot be sent to the monitor!</h4>
                                <p>Due to lack of space, the ad with id: %s of a customer with an active subscription cannot be sent to the monitor with id: %s.</p>
                            </div>
                            <a id="link-details" class='details link-text' href="%s">Check and update monitor's ads</a>
                            """,
                    params.get("adId"),
                    params.get("monitorId"),
                    params.get("link"));
        }

        @Override
        public EmailDataDto getEmailData(Map<String, String> params) {
            return null;
        }
    };

    private static String createEndDateDiv(String endDate) {
        return createEndDateDiv(endDate, "End Date");
    }

    private static String createEndDateDiv(String endDate, String label) {
        if (ObjectUtils.isEmpty(endDate)) {
            return "";
        }
        return String.format("""
                <div class="field">
                    <span class="label-campo">%s: </span>
                    <span class="valor-campo white-space-nowrap"> %s</span>
                </div>
                """, label, endDate);
    }

    private static String formatNotificationMessage(String title, String message, Map<String, String> params, String endDateDiv, String linkText) {
        return String.format("""
                <div class="informacoes">
                    <h4 id="notification-title" class="notification-title">%s</h4>
                    <p>%s</p>
                    <div class="field">
                        <span class="label-campo">Start Date: </span>
                        <span class="valor-campo white-space-nowrap"> %s</span>
                    </div>
                    %s
                    <div class="field">
                        <span class="label-campo">Services: </span>
                        <span class="valor-campo white-space-nowrap"> %s</span>
                    </div>
                </div>
                <a id="link-details" class='details link-text' href="%s">%s</a>
                <p>Need help? Contact us anytime at support@telas-ads.com</p>
                """, title, message, params.get("startDate"), endDateDiv, params.get("locations"), params.get("link"), linkText);
    }

    private static EmailDataDto createEmailData(String subject, String template, Map<String, String> params, String startDate, String endDate) {
        EmailDataDto emailData = new EmailDataDto();
        emailData.setSubject(subject);
        emailData.setTemplate(template);
        emailData.getParams().put("name", params.get("name"));
        emailData.getParams().put("locations", params.get("locations"));
        emailData.getParams().put("link", params.get("link"));
        emailData.getParams().put("startDate", startDate);
        emailData.getParams().put("endDate", ObjectUtils.isEmpty(endDate) ? "" : endDate);
        return emailData;
    }

    public abstract String getNotificationMessage(Map<String, String> params);

    public abstract EmailDataDto getEmailData(Map<String, String> params);
}