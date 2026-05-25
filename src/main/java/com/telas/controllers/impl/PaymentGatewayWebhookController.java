package com.telas.controllers.impl;

import com.telas.services.WebhookProcessingResult;
import com.telas.services.WebhookProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class PaymentGatewayWebhookController {

    private final WebhookProcessingService webhookProcessingService;

    @PostMapping
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        WebhookProcessingResult result = webhookProcessingService.processStripeWebhook(payload, sigHeader);
        return ResponseEntity.status(result.status()).body(result.body());
    }
}
