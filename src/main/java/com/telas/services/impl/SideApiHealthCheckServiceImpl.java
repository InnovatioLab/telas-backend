package com.telas.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telas.services.SideApiHealthCheckService;
import com.telas.shared.utils.ValidateDataUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SideApiHealthCheckServiceImpl implements SideApiHealthCheckService {
    private static final Logger log = LoggerFactory.getLogger(SideApiHealthCheckServiceImpl.class);

    private final ObjectMapper objectMapper;

    @Value("${monitoring.sideapi.enabled:true}")
    private boolean enabled;

    @Value("${monitoring.sideapi.port:8099}")
    private int port;

    @Value("${monitoring.sideapi.path:/health}")
    private String path;

    @Value("${monitoring.sideapi.timeout-ms:3000}")
    private long timeoutMs;

    @Override
    public SideApiHealthOutcome check(String boxIp) {
        if (!enabled) {
            return SideApiHealthOutcome.down("disabled", null);
        }
        if (ValidateDataUtils.isNullOrEmptyString(boxIp)) {
            return SideApiHealthOutcome.down("missing_ip", null);
        }

        String baseUrl = "http://" + boxIp + ":" + port;
        String p = normalizePath(path);
        String url = baseUrl + p;

        try {
            WebClient webClient = WebClient.builder().baseUrl(baseUrl).build();
            Duration timeout = Duration.ofMillis(timeoutMs);

            SideApiResponse response =
                    webClient.get()
                            .uri(p)
                            .retrieve()
                            .onStatus(HttpStatusCode::isError, r -> Mono.error(new RuntimeException("http_" + r.statusCode().value())))
                            .bodyToMono(String.class)
                            .map(body -> new SideApiResponse(body, 200))
                            .timeout(timeout)
                            .block(timeout);

            if (response == null) {
                return SideApiHealthOutcome.down("empty_response", null);
            }
            boolean up = isUpJson(objectMapper, response.body());
            return up ? SideApiHealthOutcome.up(response.httpStatus()) : SideApiHealthOutcome.down("status_not_up", response.httpStatus());
        } catch (Exception ex) {
            log.debug("sideapi.health.check.failed ip={} url={} err={}", boxIp, url, ex.getMessage());
            return SideApiHealthOutcome.down(ex.getMessage(), null);
        }
    }

    static boolean isUpJson(ObjectMapper objectMapper, String body) {
        if (ValidateDataUtils.isNullOrEmptyString(body)) {
            return false;
        }
        try {
            JsonNode node = objectMapper.readTree(body);
            JsonNode status = node.get("status");
            if (status == null || status.isNull()) {
                return false;
            }
            String s = status.asText("");
            return "UP".equalsIgnoreCase(s.trim());
        } catch (Exception e) {
            return false;
        }
    }

    private static String normalizePath(String p) {
        if (ValidateDataUtils.isNullOrEmptyString(p)) {
            return "/health";
        }
        String trimmed = p.trim();
        if (!trimmed.startsWith("/")) {
            trimmed = "/" + trimmed;
        }
        return trimmed;
    }

    private record SideApiResponse(String body, int httpStatus) {
    }
}

