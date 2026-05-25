package com.telas.notification.handlers;

import com.telas.dtos.EmailDataDto;
import com.telas.enums.NotificationReference;
import com.telas.notification.NotificationHandler;
import com.telas.notification.NotificationTemplateRegistry;
import com.telas.shared.constants.SharedConstants;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.Map;

@Component
public class SubscriptionExpiryFiveDaysNotificationHandler implements NotificationHandler {

    private final NotificationTemplateRegistry templateRegistry;

    public SubscriptionExpiryFiveDaysNotificationHandler(NotificationTemplateRegistry templateRegistry) {
        this.templateRegistry = templateRegistry;
    }

    @Override
    public NotificationReference getReference() {
        return NotificationReference.SUBSCRIPTION_ABOUT_TO_EXPIRY_5_DAYS;
    }

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
        NotificationTemplateRegistry.EmailTemplate template = templateRegistry.subscriptionExpiringCountdown(
                SharedConstants.EMAIL_SUBJECT_SUBSCRIPTION_EXPIRING_5_DAYS
        );
        return templateRegistry.buildEmail(template, Map.of(
                "name", params.getOrDefault("name", ""),
                "link", params.getOrDefault("link", ""),
                "endDate", ObjectUtils.isEmpty(params.get("endDate")) ? "" : params.get("endDate"),
                "daysRemaining", params.getOrDefault("daysRemaining", "5")
        ));
    }
}
