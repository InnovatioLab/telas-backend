package com.telas.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.telas.entities.Box;
import com.telas.monitoring.entities.BoxSubnetRouteEntity;
import com.telas.monitoring.repositories.BoxSubnetRouteEntityRepository;
import com.telas.repositories.BoxRepository;
import com.telas.services.TailscaleRoutesSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.telas.shared.utils.Ipv4CidrUtil;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class TailscaleRoutesSyncServiceImpl implements TailscaleRoutesSyncService {

    private static final Logger log = LoggerFactory.getLogger(TailscaleRoutesSyncServiceImpl.class);

    private static final String SOURCE_TAILSCALE = "TAILSCALE";

    private final TailscaleAccessTokenProvider tokenProvider;
    private final BoxRepository boxRepository;
    private final BoxSubnetRouteEntityRepository subnetRouteEntityRepository;
    private final WebClient apiClient;

    @Value("${monitoring.tailscale.enabled:false}")
    private boolean enabled;

    @Value("${monitoring.tailscale.tailnet:}")
    private String tailnet;

    public TailscaleRoutesSyncServiceImpl(
            TailscaleAccessTokenProvider tokenProvider,
            BoxRepository boxRepository,
            BoxSubnetRouteEntityRepository subnetRouteEntityRepository) {
        this.tokenProvider = tokenProvider;
        this.boxRepository = boxRepository;
        this.subnetRouteEntityRepository = subnetRouteEntityRepository;
        this.apiClient = WebClient.builder().baseUrl("https://api.tailscale.com").build();
    }

    @Override
    @Transactional
    public Map<String, Object> syncSubnetRoutes() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("tailscaleSyncAttempted", false);
        if (!enabled) {
            summary.put("tailscaleSyncSkipped", "disabled");
            return summary;
        }
        if (!StringUtils.hasText(tailnet)) {
            log.warn("tailscale.routes.sync skipped: monitoring.tailscale.tailnet not set");
            summary.put("tailscaleSyncSkipped", "no_tailnet");
            return summary;
        }
        summary.put("tailscaleSyncAttempted", true);
        final String tn = tailnet.trim();
        final String token;
        try {
            token = tokenProvider.getAccessToken();
        } catch (Exception e) {
            log.warn("tailscale.routes.sync oauth_failed", e);
            summary.put("tailscaleSyncError", "oauth_failed:" + e.getMessage());
            return summary;
        }
        JsonNode root;
        try {
            root =
                    apiClient
                            .get()
                            .uri(
                                    uriBuilder ->
                                            uriBuilder
                                                    .path("/api/v2/tailnet/{tailnet}/devices")
                                                    .queryParam("fields", "all")
                                                    .build(tn))
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .retrieve()
                            .bodyToMono(JsonNode.class)
                            .block();
        } catch (WebClientResponseException e) {
            String body =
                    e.getResponseBodyAsString() != null && e.getResponseBodyAsString().length() > 400
                            ? e.getResponseBodyAsString().substring(0, 400) + "…"
                            : e.getResponseBodyAsString();
            log.warn(
                    "tailscale.routes.sync http tailnet={} status={} body={}",
                    tn,
                    e.getStatusCode().value(),
                    body);
            summary.put("tailscaleSyncError", "http_" + e.getStatusCode().value());
            return summary;
        } catch (Exception e) {
            log.warn("tailscale.routes.sync fetch_failed tailnet={}", tn, e);
            summary.put("tailscaleSyncError", e.getMessage());
            return summary;
        }
        if (root == null || !root.has("devices")) {
            summary.put("tailscaleSyncError", "invalid_response");
            return summary;
        }
        JsonNode devices = root.get("devices");
        List<Box> boxes = boxRepository.findAllActiveWithBoxAddress();
        int matched = 0;
        int rows = 0;
        Instant syncedAt = Instant.now();
        for (Box box : boxes) {
            if (box.getBoxAddress() == null || !StringUtils.hasText(box.getBoxAddress().getIp())) {
                continue;
            }
            String boxTsIp = box.getBoxAddress().getIp().trim();
            JsonNode device = findDeviceByTailscaleIp(devices, boxTsIp);
            if (device == null) {
                continue;
            }
            matched++;
            subnetRouteEntityRepository.deleteByBox_Id(box.getId());
            List<String> advertised = stringList(device.get("advertisedRoutes"));
            List<String> enabled = stringList(device.get("enabledRoutes"));
            Set<String> cidrs = new LinkedHashSet<>();
            cidrs.addAll(Ipv4CidrUtil.filterIpv4Slash24Cidrs(advertised));
            cidrs.addAll(Ipv4CidrUtil.filterIpv4Slash24Cidrs(enabled));
            List<BoxSubnetRouteEntity> toSave = new ArrayList<>();
            for (String cidr : cidrs) {
                boolean adv = advertised.contains(cidr);
                boolean en = enabled.contains(cidr);
                BoxSubnetRouteEntity row = new BoxSubnetRouteEntity();
                row.setBox(box);
                row.setCidr(cidr);
                row.setSource(SOURCE_TAILSCALE);
                row.setAdvertised(adv);
                row.setEnabledRoute(en);
                row.setLastSyncedAt(syncedAt);
                row.setCreatedAt(syncedAt);
                row.setUpdatedAt(syncedAt);
                toSave.add(row);
            }
            subnetRouteEntityRepository.saveAll(toSave);
            rows += toSave.size();
        }
        log.info(
                "tailscale.routes.sync ok tailnet={} activeBoxes={} devicesMatched={} routeRows={}",
                tn,
                boxes.size(),
                matched,
                rows);
        summary.put("tailscaleRouteRows", rows);
        summary.put("tailscaleDevicesMatched", matched);
        summary.put("tailscaleActiveBoxes", boxes.size());
        return summary;
    }

    private static JsonNode findDeviceByTailscaleIp(JsonNode devices, String tailscaleIp) {
        if (devices == null || !devices.isArray()) {
            return null;
        }
        String want = tailscaleIp.trim().toLowerCase(Locale.ROOT);
        for (JsonNode d : devices) {
            JsonNode addrs = d.get("addresses");
            if (addrs == null || !addrs.isArray()) {
                continue;
            }
            for (JsonNode a : addrs) {
                if (a.isTextual()) {
                    String s = normalizeAddr(a.asText());
                    if (want.equals(s)) {
                        return d;
                    }
                }
            }
        }
        return null;
    }

    private static String normalizeAddr(String raw) {
        if (raw == null) {
            return "";
        }
        String t = raw.trim();
        int pct = t.indexOf('%');
        if (pct >= 0) {
            t = t.substring(0, pct);
        }
        return t.toLowerCase(Locale.ROOT);
    }

    private static List<String> stringList(JsonNode arr) {
        if (arr == null || !arr.isArray()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (JsonNode x : arr) {
            if (x.isTextual()) {
                out.add(x.asText());
            }
        }
        return out;
    }

}
