package com.marketingproject.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.marketingproject.dtos.request.MonitorRequestDto;
import com.marketingproject.dtos.response.LinkResponseDto;
import com.marketingproject.dtos.response.MonitorMinResponseDto;
import com.marketingproject.dtos.response.MonitorResponseDto;
import com.marketingproject.entities.Address;
import com.marketingproject.entities.AdvertisingAttachment;
import com.marketingproject.entities.Client;
import com.marketingproject.entities.Monitor;
import com.marketingproject.infra.exceptions.ResourceNotFoundException;
import com.marketingproject.infra.security.model.AuthenticatedUser;
import com.marketingproject.infra.security.services.AuthenticatedUserService;
import com.marketingproject.repositories.AdvertisingAttachmentRepository;
import com.marketingproject.repositories.MonitorRepository;
import com.marketingproject.services.BucketService;
import com.marketingproject.services.GeolocationService;
import com.marketingproject.services.MonitorService;
import com.marketingproject.shared.audit.CustomRevisionListener;
import com.marketingproject.shared.constants.valitation.AttachmentValidationMessages;
import com.marketingproject.shared.constants.valitation.MonitorValidationMessages;
import com.marketingproject.shared.utils.AttachmentUtils;
import com.marketingproject.shared.utils.ValidateDataUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MonitorServiceImpl implements MonitorService {
    private final AuthenticatedUserService authenticatedUserService;
    private final MonitorRepository repository;
    private final GeolocationService geolocationService;
    private final AdvertisingAttachmentRepository advertisingAttachmentRepository;
    private final BucketService bucketService;

    @Override
    @Transactional
    public void save(MonitorRequestDto request, UUID monitorId) throws JsonProcessingException {
        AuthenticatedUser authenticatedUser = authenticatedUserService.validateAdmin();
        geolocationService.getMonitorCoordinates(request);

        List<AdvertisingAttachment> advertisingAttachments = ValidateDataUtils.isNullOrEmpty(request.getAdvertisingAttachmentsIds())
                ? List.of()
                : advertisingAttachmentRepository.findAllById(request.getAdvertisingAttachmentsIds())
                .orElseThrow(() -> new ResourceNotFoundException(AttachmentValidationMessages.ATTACHMENT_NOT_FOUND));

        Monitor monitor;

        if (monitorId != null) {
            monitor = findEntityById(monitorId);
            boolean addressChanged = monitor.getAddress().hasChanged(request.getAddress());

            if (addressChanged) {
                geolocationService.getMonitorCoordinates(request);
            }

            CustomRevisionListener.setUsername(authenticatedUser.client().getBusinessName());
            CustomRevisionListener.setOldData(monitor.toStringMapper());
            monitor.update(request, advertisingAttachments);
            monitor.setUsernameUpdate(authenticatedUser.client().getBusinessName());
        } else {
            geolocationService.getMonitorCoordinates(request);
            monitor = new Monitor(request, advertisingAttachments);
            monitor.setUsernameCreate(authenticatedUser.client().getBusinessName());
        }

        repository.save(monitor);
    }

    @Override
    @Transactional(readOnly = true)
    public MonitorResponseDto findById(UUID monitorId) {
        authenticatedUserService.validateAdmin();

        Monitor entity = repository.findById(monitorId).orElseThrow(() -> new ResourceNotFoundException(MonitorValidationMessages.MONITOR_NOT_FOUND));

        List<LinkResponseDto> advertisingAttachmentLinks = entity.getAdvertisingAttachments().stream()
                .map(attachment -> new LinkResponseDto(attachment.getId(), bucketService.getLink(AttachmentUtils.format(attachment))))
                .toList();

        return new MonitorResponseDto(entity, advertisingAttachmentLinks);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MonitorMinResponseDto> findNearestActiveMonitors(String zipCode, BigDecimal sizeFilter, String typeFilter, int limit) {
        Client client = authenticatedUserService.getLoggedUser().client();
        String countryCode = client.getAddresses().stream()
                .findFirst()
                .map(Address::getCountry)
                .orElse("US");

        Map<String, Double> coordinates = geolocationService.getCoordinatesFromZipCode(zipCode, countryCode);
        double latitude = coordinates.get("latitude");
        double longitude = coordinates.get("longitude");

        return repository.findNearestActiveMonitorsWithFilters(latitude, longitude, sizeFilter, typeFilter, limit)
                .stream()
                .map(result -> new MonitorMinResponseDto(
                        result[0].toString(),
                        Boolean.parseBoolean(result[1].toString()),
                        result[2].toString(),
                        Double.parseDouble(result[3].toString()),
                        ((Number) result[6]).doubleValue()
                ))
                .toList();
    }

    Monitor findEntityById(UUID monitorId) {
        authenticatedUserService.validateAdmin();
        return repository.findById(monitorId).orElseThrow(() -> new ResourceNotFoundException(MonitorValidationMessages.MONITOR_NOT_FOUND));
    }
}
