package com.telas.services.impl;

import com.telas.dtos.request.BoxRequestDto;
import com.telas.dtos.response.BoxMonitorAdResponseDto;
import com.telas.dtos.response.BoxResponseDto;
import com.telas.dtos.response.StatusMonitorsResponseDto;
import com.telas.entities.Box;
import com.telas.entities.BoxAddress;
import com.telas.entities.Monitor;
import com.telas.helpers.BoxHelper;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.repositories.BoxRepository;
import com.telas.repositories.MonitorRepository;
import com.telas.services.BoxService;
import com.telas.shared.constants.valitation.BoxValidationMessages;
import com.telas.shared.utils.ValidateDataUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BoxServiceImpl implements BoxService {
  private final BoxRepository repository;
  private final AuthenticatedUserService authenticatedUserService;
  private final MonitorRepository monitorRepository;
  private final BoxHelper helper;

  @Override
  @Transactional(readOnly = true)
  public List<BoxResponseDto> findAll() {
    authenticatedUserService.validateAdmin();

    return repository.findAll().stream()
            .map(BoxResponseDto::new)
            .toList();
  }

  @Override
  @Transactional
  public void save(BoxRequestDto request, UUID boxId) {
    authenticatedUserService.validateAdmin();

    BoxAddress boxAddress = helper.getBoxAddress(request.getBoxAddressId());
    List<Monitor> monitors = ValidateDataUtils.isNullOrEmpty(request.getMonitorIds()) ?
            Collections.emptyList() :
            helper.getMonitors(request.getMonitorIds());

    Box box = (boxId != null) ? updateBox(request, boxId, boxAddress, monitors) : createBox(boxAddress, monitors);

    repository.save(box);
    helper.sendUpdateBoxMonitorsAdsRequest(box);
  }

  @Override
  @Transactional
  public List<BoxMonitorAdResponseDto> getMonitorsAdsByAddress(String address) {
    Box box = findByAddress(address);

    if (activateBoxIfInactive(box)) {
      repository.save(box);
    }

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

  private boolean activateBoxIfInactive(Box box) {
    if (!box.isActive()) {
      box.setActive(true);
      return true;
    }
    return false;
  }

  private Box findById(UUID boxId) {
    return repository.findById(boxId)
            .orElseThrow(() -> new ResourceNotFoundException(BoxValidationMessages.BOX_NOT_FOUND));
  }

  private Box findByAddress(String address) {
    return repository.findByAddress(address)
            .orElseThrow(() -> new ResourceNotFoundException(BoxValidationMessages.BOX_NOT_FOUND));
  }

  private Box createBox(BoxAddress boxAddress, List<Monitor> monitors) {
    helper.validateUniqueAddress(boxAddress);

    if (monitors.stream().anyMatch(monitor -> monitor.getBox() != null)) {
      throw new ResourceNotFoundException(BoxValidationMessages.MONITOR_ALREADY_ASSOCIATED);
    }

    return new Box(boxAddress, monitors);
  }

  private Box updateBox(BoxRequestDto request, UUID boxId, BoxAddress boxAddress, List<Monitor> monitors) {
    Box box = findById(boxId);

    if (!box.getBoxAddress().getId().equals(boxAddress.getId())) {
      helper.validateUniqueAddress(boxAddress);
      box.setBoxAddress(boxAddress);
    }

    box.setActive(request.isActive());
    updateMonitors(box, monitors);

    return box;
  }

  private void updateMonitors(Box box, List<Monitor> monitors) {
    List<UUID> newMonitorIds = monitors.stream().map(Monitor::getId).toList();

    box.getMonitors().removeIf(monitor -> {
      if (!newMonitorIds.contains(monitor.getId())) {
        monitor.setBox(null);
        monitorRepository.save(monitor);
        return true;
      }
      return false;
    });

    List<UUID> boxMonitorIds = box.getMonitors().stream().map(Monitor::getId).toList();
    monitors.stream()
            .filter(monitor -> !boxMonitorIds.contains(monitor.getId()))
            .forEach(monitor -> {
              monitor.setBox(box);
              monitorRepository.save(monitor);
              box.getMonitors().add(monitor);
            });
  }
}
