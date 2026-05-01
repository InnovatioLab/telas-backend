package com.telas.enums;

import com.telas.dtos.EmailDataDto;
import com.telas.shared.constants.SharedConstants;
import org.springframework.util.ObjectUtils;

import java.util.HashMap;
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
                    "Upload your attachments",
                    true
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
                    "Manage your subscriptions",
                    true
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
                    "Manage your subscriptions",
                    false
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
                    "Manage your subscriptions",
                    false
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
                    <div class="informacoes">
                        <h4 id="notification-title" class="notification-title">15 Days Before Expiration</h4>
                        <p>We hope you’ve enjoyed your Ad service with Telas. We wanted to remind you that your current service is set to expire soon.</p>
                        <div class="field">
                            <span id="attachment-name" class="field-label">Service End Date: </span>
                            <span class="field-value">%s</span>
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
    AD_RECEIVED {
        @Override
        public String getNotificationMessage(Map<String, String> params) {
            return String.format("""
                    <div class="informacoes">
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
    AD_RESUBMITTED_FOR_VALIDATION {
        @Override
        public String getNotificationMessage(Map<String, String> params) {
            String adName = params.getOrDefault("adName", "your ad");
            return String.format("""
                    <div class="informacoes">
                        <h4 id="notification-title" class="notification-title">Revised ad ready for validation</h4>
                        <p>We updated <strong>%s</strong> after your feedback. Review and approve or reject again.</p>
                    </div>
                    <p>Please use this <a id="link-details" class='details link-text' href="%s">link</a> to open My Telas (Ads tab).</p>
                    """, adName, params.get("link"));
        }

        @Override
        public EmailDataDto getEmailData(Map<String, String> params) {
            return createAdResubmittedClientEmailData(params);
        }
    },
    ADMIN_AD_RESUBMITTED_TO_CLIENT {
        @Override
        public String getNotificationMessage(Map<String, String> params) {
            return String.format("""
                    <div class="informacoes">
                        <h4 id="notification-title" class="notification-title">Ad sent back to customer</h4>
                        <p><strong>%s</strong> — ad <strong>%s</strong> was updated by <strong>%s</strong> and awaits client validation again.</p>
                    </div>
                    <a id="link-details" class='details link-text' href="%s">Open Ads management</a>
                    """,
                    params.getOrDefault("clientName", "Client"),
                    params.getOrDefault("adName", "Ad"),
                    params.getOrDefault("adminName", "Admin"),
                    params.get("link"));
        }

        @Override
        public EmailDataDto getEmailData(Map<String, String> params) {
            return createAdminAdResubmittedEmailData(params);
        }
    },
    CLIENT_AD_REJECTED {
        @Override
        public String getNotificationMessage(Map<String, String> params) {
            String name = params.getOrDefault("name", "Customer");
            String adName = params.getOrDefault("adName", "Ad");
            String link = params.getOrDefault("link", "");
            return formatNotificationMessage(
                    "Ad rejected by customer",
                    String.format("%s rejected the ad %s.", name, adName),
                    params,
                    null,
                    ObjectUtils.isEmpty(link) ? null : "Open details",
                    false
            );
        }

        @Override
        public EmailDataDto getEmailData(Map<String, String> params) {
            return createEmailData(
                    SharedConstants.EMAIL_SUBJECT_CLIENT_AD_REJECTED,
                    SharedConstants.TEMPLATE_EMAIL_CLIENT_AD_REJECTED,
                    params,
                    null,
                    null
            );
        }
    },
    MONITOR_IN_WISHLIST_NOW_AVAILABLE {
        @Override
        public String getNotificationMessage(Map<String, String> params) {
            return String.format("""
                    <div class="informacoes">
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
                            <div class="informacoes">
                                <h4 id="notification-title" class="notification-title">An approved ad cannot be sent to the monitor!</h4>
                                <p>Due to lack of space, the ad with id: %s of a customer with an active subscription cannot be sent to the monitor with id: %s.</p>
                            </div>
                            <a id="link-details" class='details link-text' href="%s">Check and update monitor's ads</a>
                            """,
                    params.get("adIds"),
                    params.get("monitorId"),
                    params.get("link"));
        }

        @Override
        public EmailDataDto getEmailData(Map<String, String> params) {
            return null;
        }
    },
    MONITORING_HOST_REBOOT {
        @Override
        public String getNotificationMessage(Map<String, String> params) {
            return String.format("""
                    <div class="informacoes">
                        <h4 id="notification-title" class="notification-title">Monitoring: host reboot detected</h4>
                        <p>Box IP <strong>%s</strong> — incident <strong>%s</strong> (severity %s). Uptime drop: %s s.</p>
                        <div class="field">
                            <span class="field-label">Notification time: </span>
                            <span class="field-value">%s</span>
                        </div>
                    </div>
                    """,
                    params.get("boxIp"),
                    params.get("incidentType"),
                    params.get("severity"),
                    params.get("uptimeDropSeconds"),
                    params.get("notifiedAt"));
        }

        @Override
        public EmailDataDto getEmailData(Map<String, String> params) {
            EmailDataDto emailData = new EmailDataDto();
            emailData.setParams(new HashMap<>());
            emailData.setSubject(SharedConstants.EMAIL_SUBJECT_HOST_REBOOT);
            emailData.setTemplate(SharedConstants.TEMPLATE_EMAIL_HOST_REBOOT);
            emailData.getParams().put("boxIp", params.getOrDefault("boxIp", ""));
            emailData.getParams().put("incidentType", params.getOrDefault("incidentType", ""));
            emailData.getParams().put("severity", params.getOrDefault("severity", ""));
            emailData.getParams().put("uptimeDropSeconds", params.getOrDefault("uptimeDropSeconds", ""));
            emailData.getParams().put("notifiedAt", params.getOrDefault("notifiedAt", ""));
            return emailData;
        }
    },
    BOX_STATUS_UPDATED {
        @Override
        public String getNotificationMessage(Map<String, String> params) {
            String downtime = params.getOrDefault("downtime", "");
            String downtimeBlock = "";
            if (downtime != null && !downtime.isBlank()) {
                downtimeBlock =
                        """
                        <div class="field">
                            <span class="field-label">Downtime: </span>
                            <span class="field-value">"""
                        + downtime
                        + """
                        </span>
                        </div>
                        """;
            }
            return String.format(
                    """
                    <div class="informacoes">
                        <h4 id="notification-title" class="notification-title">Box status updated</h4>
                        <p>The box with IP <strong>%s</strong> was <strong>%s</strong>.</p>
                        <div class="field">
                            <span class="field-label">Linked monitor address(es): </span>
                            <span class="field-value">%s</span>
                        </div>
                        <div class="field">
                            <span class="field-label">Notification time: </span>
                            <span class="field-value">%s</span>
                        </div>%s
                    </div>
                    """,
                    params.get("ip"),
                    params.get("statusLabel"),
                    params.get("monitorAddresses"),
                    params.get("notifiedAt"),
                    downtimeBlock);
        }

        @Override
        public EmailDataDto getEmailData(Map<String, String> params) {
            EmailDataDto emailData = new EmailDataDto();
            emailData.setParams(new HashMap<>());
            emailData.setSubject(SharedConstants.EMAIL_SUBJECT_BOX_STATUS_UPDATED);
            emailData.setTemplate(SharedConstants.TEMPLATE_EMAIL_BOX_STATUS_UPDATED);
            emailData.getParams().put("ip", params.get("ip"));
            emailData.getParams().put("statusLabel", params.get("statusLabel"));
            emailData.getParams().put("monitorAddresses", params.get("monitorAddresses"));
            emailData.getParams().put("notifiedAt", params.get("notifiedAt"));
            emailData.getParams().put("incidentType", params.getOrDefault("incidentType", ""));
            emailData.getParams().put("severity", params.getOrDefault("severity", ""));
            emailData.getParams().put("downtime", params.getOrDefault("downtime", ""));
            return emailData;
        }
    },
    MONITOR_STATUS_UPDATED {
        @Override
        public String getNotificationMessage(Map<String, String> params) {
            return String.format("""
                    <div class="informacoes">
                        <h4 id="notification-title" class="notification-title">Monitor status updated</h4>
                        <p>The monitor at <strong>%s</strong> was <strong>%s</strong>.</p>
                        <div class="field">
                            <span class="field-label">Notification time: </span>
                            <span class="field-value">%s</span>
                        </div>
                    </div>
                    """,
                    params.get("monitorAddress"),
                    params.get("statusLabel"),
                    params.get("notifiedAt"));
        }

        @Override
        public EmailDataDto getEmailData(Map<String, String> params) {
            EmailDataDto emailData = new EmailDataDto();
            emailData.setParams(new HashMap<>());
            emailData.setSubject(SharedConstants.EMAIL_SUBJECT_MONITOR_STATUS_UPDATED);
            emailData.setTemplate(SharedConstants.TEMPLATE_EMAIL_MONITOR_STATUS_UPDATED);
            emailData.getParams().put("monitorAddress", params.get("monitorAddress"));
            emailData.getParams().put("statusLabel", params.get("statusLabel"));
            emailData.getParams().put("notifiedAt", params.get("notifiedAt"));
            emailData.getParams().put("incidentType", params.getOrDefault("incidentType", ""));
            emailData.getParams().put("severity", params.getOrDefault("severity", ""));
            return emailData;
        }
    },
    SMART_PLUG_INCIDENT {
        @Override
        public String getNotificationMessage(Map<String, String> params) {
            return String.format("""
                    <div class="informacoes">
                        <h4 id="notification-title" class="notification-title">Smart plug alert</h4>
                        <p>Monitor %s — incident <strong>%s</strong> (severity %s).</p>
                        <div class="field">
                            <span class="field-label">Box IP: </span>
                            <span class="field-value">%s</span>
                        </div>
                        <div class="field">
                            <span class="field-label">Notification time: </span>
                            <span class="field-value">%s</span>
                        </div>
                    </div>
                    """,
                    params.getOrDefault("monitorAddress", "Unknown"),
                    params.getOrDefault("incidentType", ""),
                    params.getOrDefault("severity", ""),
                    params.getOrDefault("boxIp", ""),
                    params.getOrDefault("notifiedAt", ""));
        }

        @Override
        public EmailDataDto getEmailData(Map<String, String> params) {
            EmailDataDto emailData = new EmailDataDto();
            emailData.setParams(new HashMap<>());
            emailData.setSubject(SharedConstants.EMAIL_SUBJECT_SMART_PLUG_INCIDENT);
            emailData.setTemplate(SharedConstants.TEMPLATE_EMAIL_SMART_PLUG_INCIDENT);
            emailData.getParams().put("monitorAddress", params.getOrDefault("monitorAddress", ""));
            emailData.getParams().put("incidentType", params.getOrDefault("incidentType", ""));
            emailData.getParams().put("severity", params.getOrDefault("severity", ""));
            emailData.getParams().put("boxIp", params.getOrDefault("boxIp", ""));
            emailData.getParams().put("notifiedAt", params.getOrDefault("notifiedAt", ""));
            emailData.getParams().put("hypothesis", params.getOrDefault("hypothesis", ""));
            emailData.getParams().put("powerWatts", params.getOrDefault("powerWatts", ""));
            emailData.getParams().put("relayOn", params.getOrDefault("relayOn", ""));
            return emailData;
        }
    },
    ADMIN_NEW_PURCHASE {
        @Override
        public String getNotificationMessage(Map<String, String> params) {
            return String.format("""
                    <div class="informacoes">
                        <h4 id="notification-title" class="notification-title">New purchase</h4>
                        <p>Customer <strong>%s</strong> completed a purchase.</p>
                        <div class="field">
                            <span class="field-label">Subscription ID: </span>
                            <span class="field-value">%s</span>
                        </div>
                    </div>
                    """,
                    params.get("buyerName"),
                    params.get("subscriptionId"));
        }

        @Override
        public EmailDataDto getEmailData(Map<String, String> params) {
            return createAdminNewPurchaseEmailData(params);
        }
    },
    SIDE_API_DOWN {
        @Override
        public String getNotificationMessage(Map<String, String> params) {
            return String.format("""
                    <div class="informacoes">
                        <h4 id="notification-title" class="notification-title">Side API DOWN</h4>
                        <p>Box <strong>%s</strong> side API is <strong>DOWN</strong>.</p>
                        <div class="field">
                            <span class="field-label">Endpoint: </span>
                            <span class="field-value">%s</span>
                        </div>
                        <div class="field">
                            <span class="field-label">Detail: </span>
                            <span class="field-value">%s</span>
                        </div>
                        <div class="field">
                            <span class="field-label">Notification time: </span>
                            <span class="field-value">%s</span>
                        </div>
                    </div>
                    """,
                    params.getOrDefault("boxIp", ""),
                    params.getOrDefault("sideApiUrl", ""),
                    params.getOrDefault("detail", ""),
                    params.getOrDefault("notifiedAt", ""));
        }

        @Override
        public EmailDataDto getEmailData(Map<String, String> params) {
            return null;
        }
    },
    SIDE_API_UP {
        @Override
        public String getNotificationMessage(Map<String, String> params) {
            String downtime = params.getOrDefault("downtime", "");
            String downtimeBlock = "";
            if (downtime != null && !downtime.isBlank()) {
                downtimeBlock =
                        """
                        <div class="field">
                            <span class="field-label">Downtime: </span>
                            <span class="field-value">"""
                        + downtime
                        + """
                        </span>
                        </div>
                        """;
            }
            return String.format("""
                    <div class="informacoes">
                        <h4 id="notification-title" class="notification-title">Side API reactivated</h4>
                        <p>Box <strong>%s</strong> side API was <strong>reactivated</strong>.</p>
                        <div class="field">
                            <span class="field-label">Endpoint: </span>
                            <span class="field-value">%s</span>
                        </div>
                        <div class="field">
                            <span class="field-label">Notification time: </span>
                            <span class="field-value">%s</span>
                        </div>%s
                    </div>
                    """,
                    params.getOrDefault("boxIp", ""),
                    params.getOrDefault("sideApiUrl", ""),
                    params.getOrDefault("notifiedAt", ""),
                    downtimeBlock);
        }

        @Override
        public EmailDataDto getEmailData(Map<String, String> params) {
            return null;
        }
    },
    SUBSCRIPTION_ABOUT_TO_EXPIRY_5_DAYS {
        @Override
        public String getNotificationMessage(Map<String, String> params) {
            return String.format("""
                    <div class="informacoes">
                        <h4 id="notification-title" class="notification-title">5 Days Before Expiration</h4>
                        <p>Your Telas advertising service is ending soon.</p>
                        <div class="field">
                            <span id="attachment-name" class="field-label">Service end date: </span>
                            <span class="field-value">%s</span>
                        </div>
                    </div>
                    <p>To continue without interruption, visit this <a id="link-details" class='details link-text' href="%s">link</a>.</p>
                    """, params.get("endDate"), params.get("link"));
        }

        @Override
        public EmailDataDto getEmailData(Map<String, String> params) {
            return createCountdownExpiryEmailData(
                    SharedConstants.EMAIL_SUBJECT_SUBSCRIPTION_EXPIRING_5_DAYS,
                    SharedConstants.TEMPLATE_EMAIL_SUBSCRIPTION_EXPIRING_COUNTDOWN,
                    params
            );
        }
    },
    SUBSCRIPTION_ABOUT_TO_EXPIRY_10_DAYS {
        @Override
        public String getNotificationMessage(Map<String, String> params) {
            return String.format("""
                    <div class="informacoes">
                        <h4 id="notification-title" class="notification-title">10 Days Before Expiration</h4>
                        <p>Your Telas advertising service is ending in about two weeks.</p>
                        <div class="field">
                            <span class="field-label">Service end date: </span>
                            <span class="field-value">%s</span>
                        </div>
                    </div>
                    <p>Visit this <a id="link-details" class='details link-text' href="%s">link</a> to renew.</p>
                    """, params.get("endDate"), params.get("link"));
        }

        @Override
        public EmailDataDto getEmailData(Map<String, String> params) {
            return createCountdownExpiryEmailData(
                    SharedConstants.EMAIL_SUBJECT_SUBSCRIPTION_EXPIRING_10_DAYS,
                    SharedConstants.TEMPLATE_EMAIL_SUBSCRIPTION_EXPIRING_COUNTDOWN,
                    params
            );
        }
    },
    SUBSCRIPTION_ABOUT_TO_EXPIRY_3_DAYS {
        @Override
        public String getNotificationMessage(Map<String, String> params) {
            return String.format("""
                    <div class="informacoes">
                        <h4 id="notification-title" class="notification-title">3 Days Before Expiration</h4>
                        <p>Your Telas advertising service is ending soon.</p>
                        <div class="field">
                            <span class="field-label">Service end date: </span>
                            <span class="field-value">%s</span>
                        </div>
                    </div>
                    <p>Visit this <a id="link-details" class='details link-text' href="%s">link</a> to renew.</p>
                    """, params.get("endDate"), params.get("link"));
        }

        @Override
        public EmailDataDto getEmailData(Map<String, String> params) {
            return createCountdownExpiryEmailData(
                    SharedConstants.EMAIL_SUBJECT_SUBSCRIPTION_EXPIRING_3_DAYS,
                    SharedConstants.TEMPLATE_EMAIL_SUBSCRIPTION_EXPIRING_COUNTDOWN,
                    params
            );
        }
    },
    SUBSCRIPTION_ABOUT_TO_EXPIRY_PENULTIMATE_DAY {
        @Override
        public String getNotificationMessage(Map<String, String> params) {
            return String.format("""
                    <div class="informacoes">
                        <h4 id="notification-title" class="notification-title">Final reminder before end</h4>
                        <p>Your Telas advertising service ends on <strong>%s</strong> (tomorrow is the last day of this period).</p>
                    </div>
                    <p>Visit this <a id="link-details" class='details link-text' href="%s">link</a> to renew.</p>
                    """, params.get("endDate"), params.get("link"));
        }

        @Override
        public EmailDataDto getEmailData(Map<String, String> params) {
            return createPenultimateExpiryEmailData(params);
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
                    <span class="field-label">%s: </span>
                    <span class="field-value"> %s</span>
                </div>
                """, label, endDate);
    }

    private static String formatNotificationMessage(String title, String message, Map<String, String> params, String endDateDiv, String linkText, boolean showStartDate) {
        String startDateDiv = showStartDate ? String.format("""
                <div class="field">
                    <span class="field-label">Start Date: </span>
                    <span class="field-value"> %s</span>
                </div>
                """, params.get("startDate")) : "";

        String locations = params.get("locations");
        String servicesBlock = "";
        if (!ObjectUtils.isEmpty(locations)) {
            servicesBlock = String.format("""
                    <div class="field flex-column">
                        <span class="field-label">Services:</span>
                        <span class="field-value">%s</span>
                    </div>
                    """, locations);
        }

        String link = ObjectUtils.isEmpty(params.get("link")) ? "#" : params.get("link");
        String safeLinkText = ObjectUtils.isEmpty(linkText) ? "Open" : linkText;

        return String.format("""
                <div class="informacoes">
                    <h4 id="notification-title" class="notification-title">%s</h4>
                    <p>%s</p>
                    %s
                    %s
                    %s
                </div>
                <a id="link-details" class='details link-text' href="%s">%s</a>
                <p>Need help? Contact us anytime at support@telas-ads.com</p>
                """, title, message, startDateDiv, endDateDiv, servicesBlock, link, safeLinkText);
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

    private static EmailDataDto createAdminNewPurchaseEmailData(Map<String, String> params) {
        EmailDataDto emailData = new EmailDataDto();
        emailData.setSubject(SharedConstants.EMAIL_SUBJECT_ADMIN_NEW_PURCHASE);
        emailData.setTemplate(SharedConstants.TEMPLATE_EMAIL_ADMIN_NEW_PURCHASE);
        emailData.getParams().put("buyerName", ObjectUtils.isEmpty(params.get("buyerName")) ? "" : params.get("buyerName"));
        emailData.getParams().put("monitorsDetailHtml", ObjectUtils.isEmpty(params.get("monitorsDetailHtml")) ? "" : params.get("monitorsDetailHtml"));
        emailData.getParams().put("attachmentListHtml", ObjectUtils.isEmpty(params.get("attachmentListHtml")) ? "" : params.get("attachmentListHtml"));
        emailData.getParams().put("veiculationSummary", ObjectUtils.isEmpty(params.get("veiculationSummary")) ? "" : params.get("veiculationSummary"));
        emailData.getParams().put("subscriptionId", ObjectUtils.isEmpty(params.get("subscriptionId")) ? "" : params.get("subscriptionId"));
        return emailData;
    }

    private static EmailDataDto createCountdownExpiryEmailData(String subject, String template, Map<String, String> params) {
        EmailDataDto emailData = new EmailDataDto();
        emailData.setSubject(subject);
        emailData.setTemplate(template);
        emailData.getParams().put("name", params.get("name"));
        emailData.getParams().put("link", params.get("link"));
        emailData.getParams().put("endDate", ObjectUtils.isEmpty(params.get("endDate")) ? "" : params.get("endDate"));
        emailData.getParams().put("daysRemaining", ObjectUtils.isEmpty(params.get("daysRemaining")) ? "" : params.get("daysRemaining"));
        return emailData;
    }

    private static EmailDataDto createPenultimateExpiryEmailData(Map<String, String> params) {
        EmailDataDto emailData = new EmailDataDto();
        emailData.setSubject(SharedConstants.EMAIL_SUBJECT_SUBSCRIPTION_EXPIRING_PENULTIMATE);
        emailData.setTemplate(SharedConstants.TEMPLATE_EMAIL_SUBSCRIPTION_EXPIRING_PENULTIMATE);
        emailData.getParams().put("name", params.get("name"));
        emailData.getParams().put("link", params.get("link"));
        emailData.getParams().put("endDate", ObjectUtils.isEmpty(params.get("endDate")) ? "" : params.get("endDate"));
        return emailData;
    }

    private static EmailDataDto createAdResubmittedClientEmailData(Map<String, String> params) {
        EmailDataDto emailData = new EmailDataDto();
        emailData.setSubject(SharedConstants.EMAIL_SUBJECT_AD_RESUBMITTED_CLIENT);
        emailData.setTemplate(SharedConstants.TEMPLATE_EMAIL_AD_RESUBMITTED_CLIENT);
        emailData.setParams(new HashMap<>());
        emailData.getParams().put("name", params.getOrDefault("name", ""));
        emailData.getParams().put("adName", params.getOrDefault("adName", "Ad"));
        emailData.getParams().put("link", params.getOrDefault("link", ""));
        return emailData;
    }

    private static EmailDataDto createAdminAdResubmittedEmailData(Map<String, String> params) {
        EmailDataDto emailData = new EmailDataDto();
        emailData.setSubject(SharedConstants.EMAIL_SUBJECT_ADMIN_AD_RESUBMITTED);
        emailData.setTemplate(SharedConstants.TEMPLATE_EMAIL_ADMIN_AD_RESUBMITTED);
        emailData.setParams(new HashMap<>());
        emailData.getParams().put("clientName", params.getOrDefault("clientName", ""));
        emailData.getParams().put("adName", params.getOrDefault("adName", ""));
        emailData.getParams().put("adminName", params.getOrDefault("adminName", ""));
        emailData.getParams().put("link", params.getOrDefault("link", ""));
        return emailData;
    }

    public abstract String getNotificationMessage(Map<String, String> params);

    public abstract EmailDataDto getEmailData(Map<String, String> params);
}