package com.telas.monitoring.plug;

import com.fasterxml.jackson.databind.JsonNode;
import com.telas.monitoring.entities.SmartPlugEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "monitoring.kasa.mode", havingValue = "sidecar")
public class SidecarSmartPlugClient implements SmartPlugClient {

    private final WebClient webClient;
    private final Duration timeout;

    public SidecarSmartPlugClient(
            @Value("${monitoring.kasa.sidecar-url:http://127.0.0.1:8099}") String baseUrl,
            @Value("${monitoring.kasa.sidecar-timeout-ms:8000}") long timeoutMs) {
        this.webClient =
                WebClient.builder().baseUrl(baseUrl.replaceAll("/$", "")).build();
        this.timeout = Duration.ofMillis(timeoutMs);
    }

    @Override
    public PlugReading read(SmartPlugEntity plug, String decryptedPassword) {
        Map<String, Object> body = new HashMap<>();
        body.put("macAddress", plug.getMacAddress());
        body.put("host", plug.getLastSeenIp());
        body.put("vendor", plug.getVendor());
        body.put("username", plug.getAccountEmail());
        body.put("password", decryptedPassword != null ? decryptedPassword : "");
        try {
            JsonNode root =
                    webClient
                            .post()
                            .uri("/read")
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(body)
                            .retrieve()
                            .bodyToMono(JsonNode.class)
                            .block(timeout);
            if (root == null) {
                return PlugReading.unreachable("empty_response");
            }
            boolean reachable = root.path("reachable").asBoolean(false);
            if (!reachable) {
                return PlugReading.unreachable(
                        textOrNull(root.path("errorCode")));
            }
            return new PlugReading(
                    true,
                    readNullableBoolean(root, "relayOn"),
                    readNullableDouble(root, "powerWatts"),
                    readNullableDouble(root, "voltageVolts"),
                    readNullableDouble(root, "currentAmperes"),
                    textOrNull(root.path("errorCode")));
        } catch (WebClientResponseException e) {
            return PlugReading.unreachable("http_" + e.getStatusCode().value());
        } catch (Exception e) {
            return PlugReading.unreachable("sidecar_error");
        }
    }

    private static String textOrNull(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        String t = node.asText();
        return t.isEmpty() ? null : t;
    }

    private static Boolean readNullableBoolean(JsonNode root, String field) {
        JsonNode n = root.get(field);
        if (n == null || n.isNull()) {
            return null;
        }
        return n.asBoolean();
    }

    private static Double readNullableDouble(JsonNode root, String field) {
        JsonNode n = root.get(field);
        if (n == null || n.isNull() || !n.isNumber()) {
            return null;
        }
        return n.asDouble();
    }
}
