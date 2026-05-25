package com.telas.notification.handlers;

import com.telas.dtos.EmailDataDto;
import com.telas.enums.NotificationReference;
import com.telas.notification.NotificationHandler;
import com.telas.notification.NotificationTemplateRegistry;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AdReceivedNotificationHandler implements NotificationHandler {

    private final NotificationTemplateRegistry templateRegistry;

    public AdReceivedNotificationHandler(NotificationTemplateRegistry templateRegistry) {
        this.templateRegistry = templateRegistry;
    }

    @Override
    public NotificationReference getReference() {
        return NotificationReference.AD_RECEIVED;
    }

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
        NotificationTemplateRegistry.EmailTemplate template = templateRegistry.clientAdReceived();
        return templateRegistry.buildEmail(template, Map.of(
                "name", params.getOrDefault("name", ""),
                "link", params.getOrDefault("link", "")
        ));
    }
}
