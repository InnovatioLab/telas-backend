package com.telas.services.box;

import com.telas.dtos.request.BoxPlaylistPushRequestDto;
import com.telas.dtos.request.RemoveBoxMonitorsAdRequestDto;
import com.telas.dtos.request.UpdateBoxMonitorsAdRequestDto;
import com.telas.dtos.response.BoxPlayerSettingsResponseDto;
import com.telas.services.BoxCarouselSettingsService;
import com.telas.shared.utils.HttpClientUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BoxPlaylistClient {

    private final Logger log = LoggerFactory.getLogger(BoxPlaylistClient.class);
    private final HttpClientUtil httpClient;
    private final BoxCarouselSettingsService boxCarouselSettingsService;

    @Value("${TOKEN_SECRET}")
    private String apiKey;

    public String resolveUpdateAdsUrl(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl + "update-ads" : baseUrl + "/update-ads";
    }

    public boolean pushEmptyPlaylist(String baseUrl, UUID monitorId) {
        return pushPlaylistToBox(baseUrl, List.of(), monitorId);
    }

    public Set<String> pushPlaylistUpdates(List<UpdateBoxMonitorsAdRequestDto> requestList) {
        Set<String> successfulBaseUrls = new HashSet<>();
        if (requestList == null || requestList.isEmpty()) {
            return successfulBaseUrls;
        }

        Map<String, List<UpdateBoxMonitorsAdRequestDto>> grouped = requestList.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(UpdateBoxMonitorsAdRequestDto::getBaseUrl));

        grouped.forEach((baseUrl, group) -> {
            if (baseUrl == null || baseUrl.isBlank()) {
                log.warn("Skipping box update group with blank baseUrl, size={}", group.size());
                return;
            }
            if (pushPlaylistToBox(baseUrl, group, null)) {
                successfulBaseUrls.add(baseUrl);
            }
        });
        return successfulBaseUrls;
    }

    public boolean stageAdFile(String baseUrl, UpdateBoxMonitorsAdRequestDto dto) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return false;
        }
        String url = baseUrl.endsWith("/") ? baseUrl + "ad" : baseUrl + "/ad";
        try {
            httpClient.makePostRequest(url, List.of(dto), Void.class, null, authHeaders());
            return true;
        } catch (Exception e) {
            log.error("Error staging ad file on box, URL: {}, message: {}", url, e.getMessage());
            return false;
        }
    }

    public void pushRemoveAds(String boxIp, List<String> adNamesToRemove) {
        if (boxIp == null || boxIp.isBlank() || adNamesToRemove == null || adNamesToRemove.isEmpty()) {
            return;
        }
        String url = String.format("http://%s:8081/remove-ads", boxIp);
        RemoveBoxMonitorsAdRequestDto dto = new RemoveBoxMonitorsAdRequestDto(adNamesToRemove);
        try {
            log.info("Sending request to remove ads from box, URL: {}", url);
            httpClient.makePostRequest(url, dto, Void.class, null, authHeaders());
        } catch (Exception e) {
            log.error("Error while sending remove-ads request, URL: {}, message: {}", url, e.getMessage());
        }
    }

    public String resolveBoxBaseUrl(String ip) {
        if (ip == null || ip.isBlank()) {
            return null;
        }
        return String.format("http://%s:8081/", ip);
    }

    public List<String> getCurrentDisplayedAds(String boxIp, UUID monitorId) {
        if (boxIp == null || boxIp.isBlank()) {
            return List.of();
        }
        String url = "http://" + boxIp + ":8081/get-ads";
        try {
            log.info("Sending request to get current displayed ads from box for monitor with ID: {}, URL: {}",
                    monitorId, url);
            Object raw = httpClient.makeGetRequest(url, List.class, null, authHeaders());
            if (!(raw instanceof List<?> list)) {
                return List.of();
            }
            return list.stream().map(String::valueOf).toList();
        } catch (Exception e) {
            log.error(
                    "Error while sending request to get current displayed ads from box for monitor with ID: {}, URL: {}, message: {}",
                    monitorId, url, e.getMessage());
            throw e;
        }
    }

    private boolean pushPlaylistToBox(String baseUrl, List<UpdateBoxMonitorsAdRequestDto> ads, UUID monitorId) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return false;
        }
        String url = resolveUpdateAdsUrl(baseUrl);
        BoxPlayerSettingsResponseDto playerSettings = boxCarouselSettingsService.getSettings();
        BoxPlaylistPushRequestDto body = new BoxPlaylistPushRequestDto(ads, playerSettings);
        try {
            if (ads == null || ads.isEmpty()) {
                log.warn(
                        "SYNC_BOX: Sending empty playlist to box (monitor may have zero monitorAds or DTO filters dropped all ads). Monitor id: {}, URL: {}",
                        monitorId,
                        url);
            } else {
                log.info("Sending request to update Ads, URL: {}", url);
            }
            httpClient.makePostRequest(url, body, Void.class, null, authHeaders());
            return true;
        } catch (Exception e) {
            log.error("Error while sending playlist to box, URL: {}, message: {}", url, e.getMessage());
            return false;
        }
    }

    private Map<String, String> authHeaders() {
        return Map.of("X-API-KEY", apiKey);
    }
}
