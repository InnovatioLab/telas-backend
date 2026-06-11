package com.telas.enums;

import com.telas.dtos.EmailDataDto;
import com.telas.notification.NotificationHandlerRegistryBridge;
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
                    "Upload your attachments",
                    true
            );
        }

        @Override
        public EmailDataDto getEmailData(Map<String, String> params) {
            return EmailDataFactory.forSubscription(
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
            return EmailDataFactory.NO_EMAIL;
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
            return EmailDataFactory.NO_EMAIL;
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
            return EmailDataFactory.NO_EMAIL;
        }
    },
    SUBSCRIPTION_ABOUT_TO_EXPIRY_REMINDER {
        @Override
        public String getNotificationMessage(Map<String, String> params) {
            return NotificationHandlerRegistryBridge.find(this)
                    .map(handler -> handler.getNotificationMessage(params))
                    .orElseGet(() -> String.format("""
                    <div class="informacoes">
                        <h4 id="notification-title" class="notification-title">15 Days Before Expiration</h4>
                        <p>We hope you've enjoyed your Ad service with Telas. We wanted to remind you that your current service is set to expire soon.</p>
                        <div class="field">
                            <span id="attachment-name" class="field-label">Service End Date: </span>
                            <span class="field-value">%s</span>
                        </div>
                    </div>
                    <p>To continue enjoying our services without interruption, please visit this <a id="link-details" class='details link-text' href="%s">link</a> and renew your subscription before the end date.</p>
                    """, params.get("endDate"), params.get("link")));
        }

        @Override
        public EmailDataDto getEmailData(Map<String, String> params) {
            return NotificationHandlerRegistryBridge.find(this)
                    .map(handler -> handler.getEmailData(params))
                    .orElseGet(() -> EmailDataFactory.forSubscription(
                            SharedConstants.EMAIL_SUBJECT_SUBSCRIPTION_EXPIRING_REMINDER,
                            SharedConstants.TEMPLATE_EMAIL_SUBSCRIPTION_EXPIRING_REMINDER,
                            params,
                            null,
                            params.get("endDate")
                    ));
        }
    },
    AD_RECEIVED {
        @Override
        public String getNotificationMessage(Map<String, String> params) {
            return NotificationHandlerRegistryBridge.find(this)
                    .map(handler -> handler.getNotificationMessage(params))
                    .orElseGet(() -> String.format("""
                    <div class="informacoes">
                        <h4 id="notification-title" class="notification-title">You received a new Ad!</h4>
                        <p>Please visit this <a id="link-details" class='details link-text' href="%s">link</a> to validate it and start to use your service!</p>
                    </div>
                    """, params.get("link")));
        }

        @Override
        public EmailDataDto getEmailData(Map<String, String> params) {
            return NotificationHandlerRegistryBridge.find(this)
                    .map(handler -> handler.getEmailData(params))
                    .orElseGet(() -> EmailDataFactory.forClientAdReceived(params));
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
            return EmailDataFactory.forAdResubmittedClient(params);
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
            return EmailDataFactory.forAdminAdResubmitted(params);
        }
    },
    CLIENT_AD_REJECTED {
        @Override
        public String getNotificationMessage(Map<String, String> params) {
            String name = params.getOrDefault("name", "Customer");
            String adName = params.getOrDefault("adName", "Ad");
            String link = params.getOrDefault("link", "");
            boolean partner = EmailDataFactory.isPartnerActor(params);
            String title = partner ? "Ad rejected by partner" : "Ad rejected by customer";
            return formatNotificationMessage(
                    title,
                    String.format("%s rejected the ad %s.", name, adName),
                    params,
                    null,
                    ObjectUtils.isEmpty(link) ? null : "Open details",
                    false
            );
        }

        @Override
        public EmailDataDto getEmailData(Map<String, String> params) {
            return EmailDataFactory.forClientAdRejectedAdmin(params);
        }
    },
    CLIENT_AD_REJECTION_CONFIRMED {
        @Override
        public String getNotificationMessage(Map<String, String> params) {
            String adName = params.getOrDefault("adName", "your ad");
            return String.format("""
                    <div class="informacoes">
                        <h4 id="notification-title" class="notification-title">Feedback received</h4>
                        <p>We registered your feedback on <strong>%s</strong>. Our team will review it and may send a revised version for your approval.</p>
                    </div>
                    <p>Open <a id="link-details" class='details link-text' href="%s">My Telas — Ads</a> anytime.</p>
                    """, adName, params.getOrDefault("link", "#"));
        }

        @Override
        public EmailDataDto getEmailData(Map<String, String> params) {
            return EmailDataFactory.forClientAdRejectionConfirmed(params);
        }
    },
    CLIENT_AD_APPROVED_CONFIRMATION {
        @Override
        public String getNotificationMessage(Map<String, String> params) {
            String adName = params.getOrDefault("adName", "your ad");
            String link = params.getOrDefault("link", "#");
            if ("true".equals(params.get("partner"))) {
                if ("true".equals(params.get("liveOnScreen"))) {
                    return String.format("""
                            <div class="informacoes">
                                <h4 id="notification-title" class="notification-title">Ad approved</h4>
                                <p>Thanks! You approved <strong>%s</strong>. It is on your screen playlist.</p>
                            </div>
                            <p>Open <a id="link-details" class='details link-text' href="%s">My screens</a> to view live ads.</p>
                            """, adName, link);
                }
                return String.format("""
                        <div class="informacoes">
                            <h4 id="notification-title" class="notification-title">Ad approved</h4>
                            <p>Thanks! You approved <strong>%s</strong>. Our team will publish it to the screen next.</p>
                        </div>
                        <p>You can track status under <a id="link-details" class='details link-text' href="%s">Review ads</a>.</p>
                        """, adName, link);
            }
            return String.format("""
                    <div class="informacoes">
                        <h4 id="notification-title" class="notification-title">Ad approved</h4>
                        <p>Thanks! You approved <strong>%s</strong>.</p>
                    </div>
                    <p>You can open <a id="link-details" class='details link-text' href="%s">My Telas — Ads</a> anytime.</p>
                    """, adName, link);
        }

        @Override
        public EmailDataDto getEmailData(Map<String, String> params) {
            return EmailDataFactory.forClientAdApproved(params);
        }
    },
    ADMIN_CLIENT_AD_APPROVED {
        @Override
        public String getNotificationMessage(Map<String, String> params) {
            String clientName = params.getOrDefault("clientName", "Customer");
            String adName = params.getOrDefault("adName", "Ad");
            String link = params.getOrDefault("link", "");
            boolean partner = EmailDataFactory.isPartnerActor(params);
            String title = partner ? "Partner approved an ad" : "Customer approved an ad";
            return formatNotificationMessage(
                    title,
                    String.format("%s approved the ad %s.", clientName, adName),
                    params,
                    null,
                    ObjectUtils.isEmpty(link) ? null : "Open details",
                    false
            );
        }

        @Override
        public EmailDataDto getEmailData(Map<String, String> params) {
            return EmailDataFactory.forAdminClientAdApproved(params);
        }
    },
    ADMIN_CLIENT_FIRST_ATTACHMENTS_UPLOADED {
        @Override
        public String getNotificationMessage(Map<String, String> params) {
            String clientName = params.getOrDefault("clientName", "Customer");
            String link = params.getOrDefault("link", "");
            boolean partner = EmailDataFactory.isPartnerActor(params);
            String title = partner ? "Partner uploaded attachments" : "Customer uploaded attachments";
            return formatNotificationMessage(
                    title,
                    String.format("%s uploaded attachments for the first time.", clientName),
                    params,
                    null,
                    ObjectUtils.isEmpty(link) ? null : "Open client",
                    false
            );
        }

        @Override
        public EmailDataDto getEmailData(Map<String, String> params) {
            return EmailDataFactory.forAdminClientFirstAttachmentsUploaded(params);
        }
    },
    CLIENT_FIRST_ATTACHMENTS_UPLOADED_ACK {
        @Override
        public String getNotificationMessage(Map<String, String> params) {
            String linkLabel = "true".equals(params.get("partner")) ? "My screens" : "My Telas";
            return String.format("""
                    <div class="informacoes">
                        <h4 id="notification-title" class="notification-title">We received your files</h4>
                        <p>Your attachments were uploaded successfully. We will use them to prepare your ad.</p>
                    </div>
                    <p>Open <a id="link-details" class='details link-text' href="%s">%s</a> anytime.</p>
                    """, params.getOrDefault("link", "#"), linkLabel);
        }

        @Override
        public EmailDataDto getEmailData(Map<String, String> params) {
            return EmailDataFactory.forClientFirstAttachmentsUploadedAck(params);
        }
    },
    CLIENT_PARTNER_SUBMISSION_ACK {
        @Override
        public String getNotificationMessage(Map<String, String> params) {
            boolean createAd = "create_ad".equals(params.get("submissionKind"));
            String title = createAd ? "Create Ad request received" : "Ad upload received";
            String body = createAd
                    ? "We received your materials and instructions. Our team will create the ad for screen <strong>%s</strong>."
                    : "Thank you for uploading your Ad. We'll notify you once it has been uploaded to the screen.";
            String bodyFormatted = createAd
                    ? String.format(body, params.getOrDefault("monitorLabel", ""))
                    : body;
            return String.format("""
                    <div class="informacoes">
                        <h4 id="notification-title" class="notification-title">%s</h4>
                        <p>%s</p>
                    </div>
                    <p>Open <a id="link-details" class='details link-text' href="%s">%s</a>.</p>
                    """,
                    title,
                    bodyFormatted,
                    params.getOrDefault("link", "#"),
                    params.getOrDefault("linkLabel", "My screens"));
        }

        @Override
        public EmailDataDto getEmailData(Map<String, String> params) {
            return EmailDataFactory.forClientPartnerSubmissionAck(params);
        }
    },
    CLIENT_AD_ON_AIR {
        @Override
        public String getNotificationMessage(Map<String, String> params) {
            String adName = params.getOrDefault("adName", "your ad");
            String link = params.getOrDefault("link", "#");
            if ("true".equals(params.get("partner"))) {
                return String.format("""
                        <div class="informacoes">
                            <h4 id="notification-title" class="notification-title">Your ad is now live</h4>
                            <p><strong>%s</strong> is now on your screen playlist.</p>
                        </div>
                        <p>Open <a id="link-details" class='details link-text' href="%s">My screens</a> to view live ads.</p>
                        """, adName, link);
            }
            return String.format("""
                    <div class="informacoes">
                        <h4 id="notification-title" class="notification-title">Your ad is now live</h4>
                        <p><strong>%s</strong> is now running.</p>
                    </div>
                    <p>Open <a id="link-details" class='details link-text' href="%s">My Telas — Ads</a> for details.</p>
                    """, adName, link);
        }

        @Override
        public EmailDataDto getEmailData(Map<String, String> params) {
            return EmailDataFactory.forClientAdOnAir(params);
        }
    },
    ADMIN_AD_ON_AIR {
        @Override
        public String getNotificationMessage(Map<String, String> params) {
            String clientName = params.getOrDefault("clientName", "Customer");
            String adName = params.getOrDefault("adName", "Ad");
            String link = params.getOrDefault("link", "");
            return formatNotificationMessage(
                    "Ad is now live",
                    String.format("%s — %s is now running.", clientName, adName),
                    params,
                    null,
                    ObjectUtils.isEmpty(link) ? null : "Open details",
                    false
            );
        }

        @Override
        public EmailDataDto getEmailData(Map<String, String> params) {
            return EmailDataFactory.forAdminAdOnAir(params);
        }
    },
    CLIENT_AD_DEPLOYED_TO_BOX {
        @Override
        public String getNotificationMessage(Map<String, String> params) {
            String adName = params.getOrDefault("adName", "your ad");
            String summary = params.getOrDefault("monitorsSummary", "");
            String ends = params.getOrDefault("subscriptionEndsAt", "");
            String extra = "";
            if (!ObjectUtils.isEmpty(summary)) {
                extra += "<p><strong>Screens:</strong> " + summary + "</p>";
            }
            if (!ObjectUtils.isEmpty(ends)) {
                extra += "<p><strong>Active plan ends:</strong> " + ends + "</p>";
            }
            String link = params.getOrDefault("link", "#");
            if ("true".equals(params.get("partner"))) {
                return String.format("""
                        <div class="informacoes">
                            <h4 id="notification-title" class="notification-title">Ad sent to screen</h4>
                            <p><strong>%s</strong> was published on the screen playlist.</p>
                            %s
                        </div>
                        <p>Open <a id="link-details" class='details link-text' href="%s">My screens</a> to view live ads.</p>
                        """, adName, extra, link);
            }
            return String.format("""
                    <div class="informacoes">
                        <h4 id="notification-title" class="notification-title">Ad sent to screen</h4>
                        <p><strong>%s</strong> was deployed to your subscription screens.</p>
                        %s
                    </div>
                    <p>Open <a id="link-details" class='details link-text' href="%s">My Telas — Ads</a>.</p>
                    """, adName, extra, link);
        }

        @Override
        public EmailDataDto getEmailData(Map<String, String> params) {
            return EmailDataFactory.forClientAdDeployedToBox(params);
        }
    },
    ADMIN_CLIENT_AD_DEPLOYED_TO_BOX {
        @Override
        public String getNotificationMessage(Map<String, String> params) {
            String clientName = params.getOrDefault("clientName", "Customer");
            String adName = params.getOrDefault("adName", "Ad");
            String link = params.getOrDefault("link", "");
            return formatNotificationMessage(
                    "Ad deployed to box",
                    String.format("%s — %s was sent to the customer's screens.", clientName, adName),
                    params,
                    null,
                    ObjectUtils.isEmpty(link) ? null : "Open client messages",
                    false
            );
        }

        @Override
        public EmailDataDto getEmailData(Map<String, String> params) {
            return EmailDataFactory.forAdminClientAdDeployedToBox(params);
        }
    },
    AD_ADDED_TO_PLAYLIST_PENDING_SYNC {
        @Override
        public String getNotificationMessage(Map<String, String> params) {
            String adName = params.getOrDefault("adName", "your ad");
            String summary = params.getOrDefault("monitorsSummary", "");
            String extra = summary.isBlank() ? "" : "<p><strong>Screen:</strong> " + summary + "</p>";
            String link = params.getOrDefault("link", "#");
            return String.format("""
                    <div class="informacoes">
                        <h4 class="notification-title">Ad added to screen playlist</h4>
                        <p><strong>%s</strong> was added to the screen playlist. Box sync is pending.</p>
                        %s
                    </div>
                    <p><a class='details link-text' href="%s">View your ads</a></p>
                    """, adName, extra, link);
        }

        @Override
        public EmailDataDto getEmailData(Map<String, String> params) {
            return EmailDataFactory.forClientAdDeployedToBox(params);
        }
    },
    ADMIN_AD_ADDED_TO_PLAYLIST_PENDING_SYNC {
        @Override
        public String getNotificationMessage(Map<String, String> params) {
            String clientName = params.getOrDefault("clientName", "Customer");
            String adName = params.getOrDefault("adName", "Ad");
            String link = params.getOrDefault("link", "");
            return formatNotificationMessage(
                    "Ad added to playlist (box pending)",
                    String.format("%s — %s was saved to the screen playlist; box sync is pending.", clientName, adName),
                    params,
                    null,
                    ObjectUtils.isEmpty(link) ? null : "Open client messages",
                    false
            );
        }

        @Override
        public EmailDataDto getEmailData(Map<String, String> params) {
            return EmailDataFactory.forAdminClientAdDeployedToBox(params);
        }
    },
    PARTNER_AD_REMOVAL_REQUESTED {
        @Override
        public String getNotificationMessage(Map<String, String> params) {
            String partnerName = params.getOrDefault("partnerName", "Partner");
            String adName = params.getOrDefault("adName", "Ad");
            String screen = params.getOrDefault("screenSummary", "");
            String message = params.getOrDefault("message", "");
            String body = partnerName + " requested removal of \"" + adName + "\"";
            if (!screen.isBlank()) {
                body += " from " + screen;
            }
            if (!message.isBlank()) {
                body += ". Message: " + message;
            }
            return formatNotificationMessage(
                    "Partner ad removal request",
                    body,
                    params,
                    null,
                    "Open ad requests",
                    false
            );
        }

        @Override
        public EmailDataDto getEmailData(Map<String, String> params) {
            return EmailDataFactory.forPartnerAdRemovalRequested(params);
        }
    },
    PARTNER_AD_REMOVAL_REQUEST_CONFIRMED {
        @Override
        public String getNotificationMessage(Map<String, String> params) {
            String adName = params.getOrDefault("adName", "your ad");
            return formatNotificationMessage(
                    "Removal request received",
                    "We received your request to remove \"" + adName + "\" from the screen. Our team will review it.",
                    params,
                    null,
                    "Open My screens",
                    false
            );
        }

        @Override
        public EmailDataDto getEmailData(Map<String, String> params) {
            return EmailDataFactory.forPartnerAdRemovalConfirmed(params);
        }
    },
    MONITOR_IN_WISHLIST_NOW_AVAILABLE {
        @Override
        public String getNotificationMessage(Map<String, String> params) {
            String addresses = params.getOrDefault("monitorsAddress", "").trim();
            String body = addresses.isEmpty()
                    ? "A screen you saved in your wish list now has availability."
                    : ("These locations now have availability: " + addresses);
            return formatNotificationMessage(
                    "Wish list — screen available",
                    body,
                    params,
                    null,
                    "Open wish list",
                    false
            );
        }

        @Override
        public EmailDataDto getEmailData(Map<String, String> params) {
            return EmailDataFactory.forMonitorWishlistAvailable(params);
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
            return EmailDataFactory.NO_EMAIL;
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
            return EmailDataFactory.forHostReboot(params);
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
            return EmailDataFactory.forBoxStatusUpdated(params);
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
            return EmailDataFactory.forMonitorStatusUpdated(params);
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
            return EmailDataFactory.forSmartPlugIncident(params);
        }
    },
    ADMIN_NEW_CLIENT_REGISTERED {
        @Override
        public String getNotificationMessage(Map<String, String> params) {
            return String.format("""
                    <div class="informacoes">
                        <h4 id="notification-title" class="notification-title">New customer registration</h4>
                        <p><strong>%s</strong> registered with e-mail <strong>%s</strong>.</p>
                        <div class="field">
                            <span class="field-label">Customer ID: </span>
                            <span class="field-value">%s</span>
                        </div>
                    </div>
                    <a id="link-details" class='details link-text' href="%s">Open customer</a>
                    """,
                    params.getOrDefault("businessName", "Customer"),
                    params.getOrDefault("contactEmail", ""),
                    params.getOrDefault("clientId", ""),
                    params.getOrDefault("link", "#"));
        }

        @Override
        public EmailDataDto getEmailData(Map<String, String> params) {
            return EmailDataFactory.forAdminNewClientRegistered(params);
        }
    },
    ADMIN_NEW_PARTNER_REGISTERED {
        @Override
        public String getNotificationMessage(Map<String, String> params) {
            return String.format("""
                    <div class="informacoes">
                        <h4 id="notification-title" class="notification-title">New partner registration</h4>
                        <p><strong>%s</strong> registered as a partner with e-mail <strong>%s</strong>.</p>
                        <div class="field">
                            <span class="field-label">Partner ID: </span>
                            <span class="field-value">%s</span>
                        </div>
                    </div>
                    <a id="link-details" class='details link-text' href="%s">Open partner</a>
                    """,
                    params.getOrDefault("businessName", "Partner"),
                    params.getOrDefault("contactEmail", ""),
                    params.getOrDefault("clientId", ""),
                    params.getOrDefault("link", "#"));
        }

        @Override
        public EmailDataDto getEmailData(Map<String, String> params) {
            EmailDataDto emailData = EmailDataFactory.forAdminNewClientRegistered(params);
            emailData.setSubject(SharedConstants.EMAIL_SUBJECT_ADMIN_NEW_PARTNER_REGISTERED);
            return emailData;
        }
    },
    ADMIN_PARTNER_FOREIGN_AD_SUBMITTED {
        @Override
        public String getNotificationMessage(Map<String, String> params) {
            return String.format("""
                    <div class="informacoes">
                        <h4 id="notification-title" class="notification-title">Partner ad submitted</h4>
                        <p>Partner <strong>%s</strong> submitted an ad for screen <strong>%s</strong>.</p>
                        <div class="field">
                            <span class="field-label">Ad label: </span>
                            <span class="field-value">%s</span>
                        </div>
                        <p>Review the attachment and configure the ad before it goes on air.</p>
                    </div>
                    <p><a id="link-details" class='details link-text' href="%s">Open ads</a></p>
                    """,
                    params.getOrDefault("partnerName", ""),
                    params.getOrDefault("monitorLabel", ""),
                    params.getOrDefault("adLabel", ""),
                    params.getOrDefault("link", "#"));
        }

        @Override
        public EmailDataDto getEmailData(Map<String, String> params) {
            return EmailDataFactory.forAdminPartnerForeignAdSubmitted(params);
        }
    },
    ADMIN_PARTNER_PLACEMENT_REQUEST {
        @Override
        public String getNotificationMessage(Map<String, String> params) {
            String monitorLabel = params.getOrDefault("monitorLabel", params.getOrDefault("monitorsSummary", ""));
            return String.format("""
                    <div class="informacoes">
                        <h4 id="notification-title" class="notification-title">Partner Create Ad request</h4>
                        <p>Partner <strong>%s</strong> submitted a Create Ad request for screen <strong>%s</strong>.</p>
                    </div>
                    <p><a id="link-details" class='details link-text' href="%s">Open partner ad requests</a></p>
                    """,
                    params.getOrDefault("partnerName", ""),
                    monitorLabel,
                    params.getOrDefault("link", "#"));
        }

        @Override
        public EmailDataDto getEmailData(Map<String, String> params) {
            return EmailDataFactory.forAdminPartnerPlacementRequest(params);
        }
    },
    ADMIN_PARTNER_FINISHED_AD_SUBMITTED {
        @Override
        public String getNotificationMessage(Map<String, String> params) {
            return String.format("""
                    <div class="informacoes">
                        <h4 id="notification-title" class="notification-title">Partner Finished Ad submitted</h4>
                        <p>Partner <strong>%s</strong> submitted a Finished Ad for screen <strong>%s</strong>.</p>
                    </div>
                    <p><a id="link-details" class='details link-text' href="%s">Open partner ad requests</a></p>
                    """,
                    params.getOrDefault("partnerName", ""),
                    params.getOrDefault("monitorLabel", ""),
                    params.getOrDefault("link", "#"));
        }

        @Override
        public EmailDataDto getEmailData(Map<String, String> params) {
            return EmailDataFactory.forAdminPartnerFinishedAdSubmitted(params);
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
            return EmailDataFactory.forAdminNewPurchase(params);
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
            return EmailDataFactory.NO_EMAIL;
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
            return EmailDataFactory.NO_EMAIL;
        }
    },
    SUBSCRIPTION_ABOUT_TO_EXPIRY_5_DAYS {
        @Override
        public String getNotificationMessage(Map<String, String> params) {
            return NotificationHandlerRegistryBridge.find(this)
                    .map(handler -> handler.getNotificationMessage(params))
                    .orElseGet(() -> String.format("""
                    <div class="informacoes">
                        <h4 id="notification-title" class="notification-title">5 Days Before Expiration</h4>
                        <p>Your Telas advertising service is ending soon.</p>
                        <div class="field">
                            <span id="attachment-name" class="field-label">Service end date: </span>
                            <span class="field-value">%s</span>
                        </div>
                    </div>
                    <p>To continue without interruption, visit this <a id="link-details" class='details link-text' href="%s">link</a>.</p>
                    """, params.get("endDate"), params.get("link")));
        }

        @Override
        public EmailDataDto getEmailData(Map<String, String> params) {
            return NotificationHandlerRegistryBridge.find(this)
                    .map(handler -> handler.getEmailData(params))
                    .orElseGet(() -> EmailDataFactory.forCountdownExpiry(
                            SharedConstants.EMAIL_SUBJECT_SUBSCRIPTION_EXPIRING_5_DAYS,
                            SharedConstants.TEMPLATE_EMAIL_SUBSCRIPTION_EXPIRING_COUNTDOWN,
                            params
                    ));
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
            return EmailDataFactory.forCountdownExpiry(
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
            return EmailDataFactory.forCountdownExpiry(
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
            return EmailDataFactory.forPenultimateExpiry(params);
        }
    },
    AD_REQUEST_QUESTIONNAIRE_UPDATED {
        @Override
        public String getNotificationMessage(Map<String, String> params) {
            String clientName = params.getOrDefault("clientName", "Customer");
            String revision = params.getOrDefault("revisionVersion", "");
            String link = params.getOrDefault("link", "");
            return formatNotificationMessage(
                    "Business questionnaire updated",
                    String.format("%s updated the business questionnaire (revision %s).", clientName, revision),
                    params,
                    null,
                    ObjectUtils.isEmpty(link) ? null : "Open Ads management",
                    false
            );
        }

        @Override
        public EmailDataDto getEmailData(Map<String, String> params) {
            return EmailDataFactory.forAdminAdRequestQuestionnaireUpdated(params);
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

        String safeTitle = ObjectUtils.isEmpty(title) ? "" : title;
        String safeMessage = ObjectUtils.isEmpty(message) ? "" : message;
        String safeStartDateDiv = ObjectUtils.isEmpty(startDateDiv) ? "" : startDateDiv;
        String safeEndDateDiv = ObjectUtils.isEmpty(endDateDiv) ? "" : endDateDiv;
        String safeServicesBlock = ObjectUtils.isEmpty(servicesBlock) ? "" : servicesBlock;

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
                """, safeTitle, safeMessage, safeStartDateDiv, safeEndDateDiv, safeServicesBlock, link, safeLinkText);
    }

    public abstract String getNotificationMessage(Map<String, String> params);

    public abstract EmailDataDto getEmailData(Map<String, String> params);
}
