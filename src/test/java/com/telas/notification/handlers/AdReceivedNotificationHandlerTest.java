package com.telas.notification.handlers;

import com.telas.notification.NotificationTemplateRegistry;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AdReceivedNotificationHandlerTest {

    @Test
    void adReceived_mustProvideEmailData() {
        var handler = new AdReceivedNotificationHandler(new NotificationTemplateRegistry());
        var emailData = handler.getEmailData(Map.of("link", "https://example.test"));
        assertThat(emailData).as("AD_RECEIVED deve gerar e-mail para o cliente").isNotNull();
    }
}
