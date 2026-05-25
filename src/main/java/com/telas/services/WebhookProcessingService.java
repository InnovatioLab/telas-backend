package com.telas.services;

public interface WebhookProcessingService {

    WebhookProcessingResult processStripeWebhook(String payload, String signatureHeader);
}
