package com.telas.enums;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationReferenceAdReceivedEmailTest {

    @Test
    void adReceived_mustProvideEmailData() {
        var emailData = NotificationReference.AD_RECEIVED.getEmailData(Map.of("link", "https://example.test"));
        assertThat(emailData).as("AD_RECEIVED deve gerar e-mail para o cliente").isNotNull();
    }
}

