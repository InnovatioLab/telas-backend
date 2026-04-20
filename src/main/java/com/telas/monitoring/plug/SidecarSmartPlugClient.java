package com.telas.monitoring.plug;

import com.fasterxml.jackson.databind.JsonNode;
import com.telas.monitoring.entities.SmartPlugEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(SidecarSmartPlugClient.class);

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
    public PlugReading read(SmartPlugEntity plug, SmartPlugCredentials credentials) {
        return readAtHost(plug, plug.getLastSeenIp(), credentials);
    }

    @Override
    public PlugReading readAtHost(SmartPlugEntity plug, String host, SmartPlugCredentials credentials) {
        if (host == null || host.isBlank()) {
            log.warn(
                    "smartPlug.read.missingHost plugId={} mac={} vendor={} monitorId={} boxId={}",
                    plug.getId(),
                    plug.getMacAddress(),
                    plug.getVendor(),
                    plug.getMonitor() != null ? plug.getMonitor().getId() : null,
                    plug.getBox() != null ? plug.getBox().getId() : null);
            return PlugReading.unreachable("missing_host");
        }
        Map<String, Object> body = new HashMap<>();
        body.put("macAddress", plug.getMacAddress());
        body.put("host", host);
        body.put("vendor", plug.getVendor());
        String username = credentials != null ? credentials.username() : null;
        String password = credentials != null ? credentials.password() : null;
        body.put("username", username != null ? username : "");
        body.put("password", password != null ? password : "");
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
            String responseBody = e.getResponseBodyAsString();
            String bodyHint = responseBody != null && responseBody.length() > 300
                    ? responseBody.substring(0, 300) + "…"
                    : responseBody;
            log.warn(
                    "smartPlug.read.sidecarHttp plugId={} mac={} vendor={} host={} status={} body={}",
                    plug.getId(),
                    plug.getMacAddress(),
                    plug.getVendor(),
                    host,
                    e.getStatusCode().value(),
                    bodyHint);
            return PlugReading.unreachable("http_" + e.getStatusCode().value());
        } catch (Exception e) {
            log.warn(
                    "smartPlug.read.sidecarError plugId={} mac={} vendor={} host={}",
                    plug.getId(),
                    plug.getMacAddress(),
                    plug.getVendor(),
                    host,
                    e);
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
