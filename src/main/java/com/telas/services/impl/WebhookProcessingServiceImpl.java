package com.telas.services.impl;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import com.telas.services.EventService;
import com.telas.services.MessageSender;
import com.telas.services.WebhookProcessingResult;
import com.telas.services.WebhookProcessingService;
import com.telas.shared.utils.ValidateDataUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WebhookProcessingServiceImpl implements WebhookProcessingService {

    private static final Logger log = LoggerFactory.getLogger(WebhookProcessingServiceImpl.class);

    private final EventService eventService;
    private final MessageSender messageSender;

    @Value("${payment.gateway.webhook.secret}")
    private String webhookSecret;

    @Override
    @Transactional(readOnly = true)
    public WebhookProcessingResult processStripeWebhook(String payload, String signatureHeader) {
        if (ValidateDataUtils.isNullOrEmptyString(webhookSecret)
                || ValidateDataUtils.isNullOrEmptyString(signatureHeader)) {
            return new WebhookProcessingResult(
                    HttpStatus.BAD_REQUEST,
                    "Webhook secret ou cabeçalho de assinatura não configurados");
        }

        Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.error("[WEBHOOK]: Error during webhook signature verification: {}", e.getMessage());
            return new WebhookProcessingResult(HttpStatus.BAD_REQUEST, "Webhook signature verification failed");
        }

        if (eventService.checkIfExists(event.getId())) {
            log.info("[WEBHOOK]: Event with ID {} has already been processed", event.getId());
            return new WebhookProcessingResult(HttpStatus.OK, "Event already processed");
        }

        log.info("[WEBHOOK]: Sending event with ID: {} to queue", event.getId());
        messageSender.sendEvent(event);
        return new WebhookProcessingResult(HttpStatus.OK, "Webhook event processed successfully");
    }
}
