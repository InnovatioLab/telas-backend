package com.marketingproject.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.marketingproject.dtos.request.MonitorRequestDto;
import com.marketingproject.entities.AdvertisingAttachment;
import com.marketingproject.entities.Monitor;
import com.marketingproject.infra.exceptions.ResourceNotFoundException;
import com.marketingproject.infra.security.model.AuthenticatedUser;
import com.marketingproject.infra.security.services.AuthenticatedUserService;
import com.marketingproject.repositories.AdvertisingAttachmentRepository;
import com.marketingproject.repositories.MonitorRepository;
import com.marketingproject.services.GeolocationService;
import com.marketingproject.services.MonitorService;
import com.marketingproject.shared.audit.CustomRevisionListener;
import com.marketingproject.shared.constants.valitation.AttachmentValidationMessages;
import com.marketingproject.shared.constants.valitation.MonitorValidationMessages;
import com.marketingproject.shared.utils.ValidateDataUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MonitorServiceImpl implements MonitorService {
    private final AuthenticatedUserService authenticatedUserService;
    private final MonitorRepository repository;
    private final GeolocationService geolocationService;
    private final AdvertisingAttachmentRepository advertisingAttachmentRepository;

    @Override
    @Transactional
    public void save(MonitorRequestDto request, UUID monitorId) throws JsonProcessingException {
        AuthenticatedUser authenticatedUser = authenticatedUserService.validateAdmin();
        geolocationService.getMonitorCoordinates(request);

        List<AdvertisingAttachment> advertisingAttachments = ValidateDataUtils.isNullOrEmpty(request.getAdvertisingAttachmentsIds())
                ? List.of()
                : advertisingAttachmentRepository.findAllByIdIn(request.getAdvertisingAttachmentsIds())
                .orElseThrow(() -> new ResourceNotFoundException(AttachmentValidationMessages.ATTACHMENT_NOT_FOUND));

        Monitor monitor;

        if (monitorId != null) {
            monitor = findById(monitorId);
            boolean addressChanged = monitor.getAddress().hasChanged(request.getAddress());

            if (addressChanged) {
                geolocationService.getMonitorCoordinates(request);
            }

            CustomRevisionListener.setUsername(authenticatedUser.client().getBusinessName());
            CustomRevisionListener.setOldData(monitor.toStringMapper());
            monitor.update(request, advertisingAttachments);
        } else {
            geolocationService.getMonitorCoordinates(request);
            monitor = new Monitor(request, advertisingAttachments);
        }

        repository.save(monitor);
    }

    @Override
    @Transactional(readOnly = true)
    public Monitor findById(UUID monitorId) {
        authenticatedUserService.validateAdmin();
        return repository.findById(monitorId).orElseThrow(() -> new ResourceNotFoundException(MonitorValidationMessages.MONITOR_NOT_FOUND));
    }
}
