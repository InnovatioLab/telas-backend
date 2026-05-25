package com.telas.notification.handlers;

import com.telas.dtos.EmailDataDto;
import com.telas.enums.NotificationReference;
import com.telas.notification.NotificationHandler;
import com.telas.notification.NotificationTemplateRegistry;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.Map;

@Component
public class SubscriptionExpiryReminderNotificationHandler implements NotificationHandler {

    private final NotificationTemplateRegistry templateRegistry;

    public SubscriptionExpiryReminderNotificationHandler(NotificationTemplateRegistry templateRegistry) {
        this.templateRegistry = templateRegistry;
    }

    @Override
    public NotificationReference getReference() {
        return NotificationReference.SUBSCRIPTION_ABOUT_TO_EXPIRY_REMINDER;
    }

    @Override
    public String getNotificationMessage(Map<String, String> params) {
        return String.format("""
                <div class="informacoes">
                    <h4 id="notification-title" class="notification-title">15 Days Before Expiration</h4>
                    <p>We hope you've enjoyed your Ad service with Telas. We wanted to remind you that your current service is set to expire soon.</p>
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
        NotificationTemplateRegistry.EmailTemplate template = templateRegistry.subscriptionExpiringReminder();
        return templateRegistry.buildEmail(template, Map.of(
                "name", params.getOrDefault("name", ""),
                "locations", params.getOrDefault("locations", ""),
                "link", params.getOrDefault("link", ""),
                "startDate", "",
                "endDate", ObjectUtils.isEmpty(params.get("endDate")) ? "" : params.get("endDate")
        ));
    }
}
