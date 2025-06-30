package com.telas.services.impl;

import com.telas.dtos.request.BoxRequestDto;
import com.telas.dtos.response.BoxMonitorAdResponseDto;
import com.telas.dtos.response.StatusMonitorsResponseDto;
import com.telas.entities.Box;
import com.telas.entities.Ip;
import com.telas.entities.Monitor;
import com.telas.helpers.BoxRequestHelper;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.repositories.BoxRepository;
import com.telas.services.BoxService;
import com.telas.shared.constants.valitation.BoxValidationMessages;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BoxServiceImpl implements BoxService {
  private final BoxRepository repository;
  private final AuthenticatedUserService authenticatedUserService;
  private final BoxRequestHelper helper;

  @Override
  @Transactional
  public void save(BoxRequestDto request, UUID boxId) {
    authenticatedUserService.validateAdmin();

    Ip ip = helper.getIp(request.getIp());
    List<Monitor> monitors = helper.getMonitors(request.getMonitorIds());
    Box box = (boxId != null) ? updateBox(request, boxId, ip, monitors) : createBox(request, ip, monitors);

    repository.save(box);
    helper.sendUpdateBoxMonitorsAdsRequest(box, monitors);
  }

  @Override
  @Transactional(readOnly = true)
  public List<BoxMonitorAdResponseDto> getMonitorsAdsByIp(String ip) {
    Box box = findByIp(ip);
    return helper.getBoxMonitorAdResponse(box);
  }

  @Override
  @Transactional
  public void checkMonitorsHealth(List<StatusMonitorsResponseDto> responseList) {
    if (responseList.isEmpty() || responseList.stream().allMatch(r -> r.getStatusCode() == 200)) {
      return;
    }
    helper.checkMonitorsHealth(responseList);
  }

  private Box findById(UUID boxId) {
    return repository.findById(boxId)
            .orElseThrow(() -> new ResourceNotFoundException(BoxValidationMessages.BOX_NOT_FOUND));
  }

  private Box findByIp(String ip) {
    return repository.findByIp(ip)
            .orElseThrow(() -> new ResourceNotFoundException(BoxValidationMessages.BOX_NOT_FOUND));
  }

  private Box createBox(BoxRequestDto request, Ip ip, List<Monitor> monitors) {
    helper.validateUniqueBoxByIp(request.getIp());

    if (monitors.stream().anyMatch(monitor -> monitor.getBox() != null)) {
      throw new ResourceNotFoundException(BoxValidationMessages.MONITOR_ALREADY_ASSOCIATED);
    }

    Box box = new Box(ip, monitors);
    monitors.forEach(monitor -> monitor.setBox(box));
    return box;
  }

  private Box updateBox(BoxRequestDto request, UUID boxId, Ip ip, List<Monitor> monitors) {
    Box box = findById(boxId);

    if (!box.getIp().getIpAddress().equals(request.getIp())) {
      helper.validateUniqueBoxByIp(request.getIp());
      box.setIp(ip);
    }

    box.setActive(request.isActive());
    updateMonitors(box, monitors);

    return box;
  }

  private void updateMonitors(Box box, List<Monitor> monitors) {
    box.getMonitors().removeIf(monitor -> {
      if (!monitors.contains(monitor)) {
        monitor.setBox(null);
        return true;
      }
      return false;
    });

    monitors.stream()
            .filter(monitor -> !box.getMonitors().contains(monitor))
            .forEach(monitor -> {
              monitor.setBox(box);
              box.getMonitors().add(monitor);
            });
  }
}
