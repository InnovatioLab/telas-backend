package com.telas.helpers;

import com.telas.dtos.request.UpdateBoxMonitorsAdRequestDto;
import com.telas.dtos.response.BoxMonitorAdResponseDto;
import com.telas.dtos.response.MonitorAdResponseDto;
import com.telas.dtos.response.StatusMonitorsResponseDto;
import com.telas.entities.Box;
import com.telas.entities.Ip;
import com.telas.entities.Monitor;
import com.telas.enums.AdValidationType;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.repositories.BoxRepository;
import com.telas.repositories.IpRepository;
import com.telas.repositories.MonitorRepository;
import com.telas.services.BucketService;
import com.telas.shared.constants.valitation.BoxValidationMessages;
import com.telas.shared.constants.valitation.MonitorValidationMessages;
import com.telas.shared.utils.AttachmentUtils;
import com.telas.shared.utils.HttpClientUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BoxRequestHelper {
  private final Logger log = LoggerFactory.getLogger(BoxRequestHelper.class);
  private final IpRepository ipRepository;
  private final BoxRepository boxRepository;
  private final MonitorRepository monitorRepository;
  private final BucketService bucketService;
  private final HttpClientUtil httpClient;

  @Transactional(readOnly = true)
  public void validateUniqueBoxByIp(String ip) {
    if (boxRepository.findByIp(ip).isPresent()) {
      throw new BusinessRuleException(BoxValidationMessages.BOX_ALREADY_EXISTS_FOR_IP);
    }
  }

  @Transactional(readOnly = true)
  public Ip getIp(String ip) {
    return ipRepository.findByIpAddress(ip)
            .orElseGet(() -> ipRepository.save(new Ip(ip)));
  }

  @Transactional(readOnly = true)
  public List<Monitor> getMonitors(List<UUID> monitorIds) {
    return monitorRepository.findAllById(monitorIds);
  }

  @Transactional(readOnly = true)
  public Monitor findMonitorById(UUID monitorId) {
    return monitorRepository.findById(monitorId)
            .orElseThrow(() -> new ResourceNotFoundException(MonitorValidationMessages.MONITOR_NOT_FOUND));
  }

  @Transactional(readOnly = true)
  public List<BoxMonitorAdResponseDto> getBoxMonitorAdResponse(Box box) {
    return box.getMonitors().stream()
            .map(monitor -> new BoxMonitorAdResponseDto(
                    monitor,
                    monitor.getMonitorAds().stream()
                            .map(monitorAd -> new MonitorAdResponseDto(
                                    monitorAd,
                                    bucketService.getLink(AttachmentUtils.format(monitorAd.getAd()))
                            ))
                            .toList()
            ))
            .toList();
  }

  @Transactional
  public void checkMonitorsHealth(List<StatusMonitorsResponseDto> responseList) {
    responseList.stream()
            .filter(response -> response.getStatusCode() != 200)
            .forEach(response -> {
              if (Objects.equals(response.getErrorLevel(), "high")) {
                Monitor monitor = findMonitorById(response.getId());

                if (monitor.isActive()) {
                  monitor.setActive(false);
                  log.error("Monitor with ID {} is sending HIGH error level due to health check failure and will be inactivated! message: {}", monitor.getId(), response.getMessage());
                  monitorRepository.save(monitor);
                }
              } else {
                log.warn("Monitor with ID {} is sending MODERATE error level due to health check failure! message: {}", response.getId(), response.getMessage());
              }
            });
  }

  @Transactional
  public void sendUpdateBoxMonitorsAdsRequest(Box box) {
    if (!box.isActive()) {
      return;
    }

    String url;
    List<UpdateBoxMonitorsAdRequestDto> body = box.getMonitors().stream()
            .flatMap(monitor -> monitor.getAds().stream()
                    .filter(ad -> AdValidationType.APPROVED.equals(ad.getValidation()))
                    .map(ad -> new UpdateBoxMonitorsAdRequestDto(
                            monitor.getId(),
                            ad.getName(),
                            bucketService.getLink(AttachmentUtils.format(ad))
                    ))
            )
            .toList();

    if (body.isEmpty()) {
      url = "http://" + box.getIp().getIpAddress() + ":5050/create-folders";
      body = box.getMonitors().stream()
              .map(monitor -> new UpdateBoxMonitorsAdRequestDto(
                      monitor.getId(),
                      null,
                      null
              ))
              .toList();
    } else {
      url = "http://" + box.getIp().getIpAddress() + ":5050/update-folders";
    }

    try {
      String action = url.contains("create-folders") ? "create folders" : "update folders";
      log.info("Sending box {} request to boxId: {}, URL: {}, body: {}", action, box.getId(), url, body);

      httpClient.makePostRequest(url, body, Void.class, null);
    } catch (Exception e) {
      String action = url.contains("create-folders") ? "create folders" : "update folders";
      log.error("Failed to send box {} request to boxId: {}, URL: {}, body: {}, error: {}", action, box.getId(), url, body, e.getMessage());
      throw e;
    }
  }
}
