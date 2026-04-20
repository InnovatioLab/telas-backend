package com.telas.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.Objects;

@Component
public class TailscaleAccessTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(TailscaleAccessTokenProvider.class);

    private static final String TOKEN_URL = "https://login.tailscale.com/api/oauth/token";

    private final WebClient webClient;

    @Value("${monitoring.tailscale.oauth.client-id:}")
    private String clientId;

    @Value("${monitoring.tailscale.oauth.client-secret:}")
    private String clientSecret;

    private volatile String cachedToken;
    private volatile Instant expiresAt = Instant.EPOCH;

    public TailscaleAccessTokenProvider() {
        this.webClient = WebClient.builder().baseUrl("https://login.tailscale.com").build();
    }

    public synchronized String getAccessToken() {
        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret)) {
            throw new IllegalStateException("tailscale_oauth_not_configured");
        }
        Instant now = Instant.now();
        if (cachedToken != null && expiresAt != null && now.isBefore(expiresAt.minusSeconds(60))) {
            return cachedToken;
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId.trim());
        form.add("client_secret", clientSecret.trim());
        form.add("grant_type", "client_credentials");
        JsonNode root =
                webClient
                        .post()
                        .uri("/api/oauth/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .bodyValue(form)
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .block();
        if (root == null || !root.hasNonNull("access_token")) {
            throw new IllegalStateException("tailscale_oauth_empty_token");
        }
        cachedToken = root.get("access_token").asText();
        int expiresIn = root.path("expires_in").asInt(3600);
        expiresAt = now.plusSeconds(Math.max(120, expiresIn));
        log.debug("tailscale.oauth.token_refreshed expiresInSeconds={}", expiresIn);
        return Objects.requireNonNull(cachedToken);
    }
}
