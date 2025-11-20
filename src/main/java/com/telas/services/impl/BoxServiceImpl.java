package com.telas.services.impl;

import com.telas.dtos.request.BoxRequestDto;
import com.telas.dtos.request.StatusBoxMonitorsRequestDto;
import com.telas.dtos.response.BoxMonitorAdResponseDto;
import com.telas.dtos.response.BoxResponseDto;
import com.telas.entities.Box;
import com.telas.entities.BoxAddress;
import com.telas.entities.Monitor;
import com.telas.enums.DefaultStatus;
import com.telas.helpers.BoxHelper;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.repositories.BoxRepository;
import com.telas.repositories.MonitorRepository;
import com.telas.services.BoxService;
import com.telas.shared.constants.valitation.BoxValidationMessages;
import com.telas.shared.constants.valitation.MonitorValidationMessages;
import com.telas.shared.utils.ValidateDataUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
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
        Monitor monitor = findMonitorById(request.getMonitorId());

        Box box = (boxId != null) ? updateBox(request, boxId, boxAddress, monitor) : new Box(boxAddress, monitor);

        repository.save(box);
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
    public void updateHealth(StatusBoxMonitorsRequestDto request) {
        boolean isActive = DefaultStatus.ACTIVE.equals(request.getStatus());

        if (!ValidateDataUtils.isNullOrEmptyString(request.getIp())) {
            Box box = findByAddress(request.getIp());
            box.setActive(isActive);
            repository.save(box);

            box.getMonitors().forEach(monitor -> monitor.setActive(isActive));
            monitorRepository.saveAll(box.getMonitors());
        }

        if (Objects.nonNull(request.getMonitorId())) {
            Monitor monitor = findMonitorById(request.getMonitorId());
            monitor.setActive(isActive);
            monitorRepository.save(monitor);
        }
    }


    private Monitor findMonitorById(UUID monitorId) {
        return monitorRepository.findById(monitorId).orElseThrow(() -> new ResourceNotFoundException(MonitorValidationMessages.MONITOR_NOT_FOUND));
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

    private Box updateBox(BoxRequestDto request, UUID boxId, BoxAddress boxAddress, Monitor monitor) {
        Box box = findById(boxId);

        if (!box.getBoxAddress().getId().equals(boxAddress.getId())) {
            box.setBoxAddress(boxAddress);
        }

        box.setActive(request.isActive());

        if (monitor.getBox() != null && !monitor.getBox().getId().equals(box.getId())) {
            Box previousBox = monitor.getBox();
            previousBox.getMonitors().removeIf(m -> m.getId().equals(monitor.getId()));
        }

        box.getMonitors().forEach(m -> {
            m.setBox(null);
            monitorRepository.save(m);
        });

        box.getMonitors().clear();

        monitor.setBox(box);
        monitorRepository.save(monitor);

        box.getMonitors().add(monitor);

        return box;

    }
}
