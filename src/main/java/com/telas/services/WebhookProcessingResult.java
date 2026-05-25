package com.telas.services;

import org.springframework.http.HttpStatus;

public record WebhookProcessingResult(HttpStatus status, String body) {}
