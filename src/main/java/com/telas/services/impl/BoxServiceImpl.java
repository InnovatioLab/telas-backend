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
//    Adicionar validação para só ter 1 box por IP
    Ip ip = helper.getIp(request.getIp());
    List<Monitor> monitors = helper.getMonitors(request.getMonitorIds());
    Box box = new Box(ip, monitors);
    monitors.forEach(monitor -> monitor.setBox(box));
    repository.save(box);
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

  private Box findByIp(String ip) {
    return repository.findByIp(ip)
            .orElseThrow(() -> new ResourceNotFoundException(BoxValidationMessages.BOX_NOT_FOUND));
  }
}
