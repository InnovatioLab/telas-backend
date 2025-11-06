package com.telas.controllers.impl;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import com.telas.services.EventService;
import com.telas.services.MessageSender;
import com.telas.shared.utils.ValidateDataUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class PaymentGatewayWebhookController {
    private final Logger log = LoggerFactory.getLogger(PaymentGatewayWebhookController.class);
    private final EventService eventService;
    private final MessageSender messageSender;

    @Value("${payment.gateway.webhook.secret}")
    private String webhookSecret;

    @Transactional(readOnly = true)
    @PostMapping
    public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
        if (ValidateDataUtils.isNullOrEmptyString(webhookSecret) || ValidateDataUtils.isNullOrEmptyString(sigHeader)) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Webhook secret ou cabeçalho de assinatura não configurados");
        }

        Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.error("[WEBHOOK CONTROLLER]: Error during webhook signature verification: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Webhook signature verification failed");
        }

        if (eventService.checkIfExists(event.getId())) {
            log.info("[WEBHOOK CONTROLLER]: Event with ID {} has already been processed", event.getId());
            return ResponseEntity.ok("Event already processed");
        }


        log.info("[WEBHOOK CONTROLLER]: Sending event with ID: {} to queue", event.getId());
        messageSender.sendEvent(event);

        return ResponseEntity.ok("Webhook event processed successfully");
    }
}
