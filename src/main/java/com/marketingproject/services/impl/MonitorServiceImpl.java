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
import com.marketingproject.enums.AttachmentValidationType;
import com.marketingproject.enums.Role;
import com.marketingproject.infra.exceptions.ResourceNotFoundException;
import com.marketingproject.infra.security.model.AuthenticatedUser;
import com.marketingproject.infra.security.services.AuthenticatedUserService;
import com.marketingproject.repositories.AdvertisingAttachmentRepository;
import com.marketingproject.repositories.MonitorRepository;
import com.marketingproject.services.BucketService;
import com.marketingproject.services.ClientService;
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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MonitorServiceImpl implements MonitorService {
    private final AuthenticatedUserService authenticatedUserService;
    private final MonitorRepository repository;
    private final GeolocationService geolocationService;
    private final AdvertisingAttachmentRepository advertisingAttachmentRepository;
    private final BucketService bucketService;
    private final ClientService clientService;

    @Override
    @Transactional
    public void save(MonitorRequestDto request, UUID monitorId) throws JsonProcessingException {
        AuthenticatedUser authenticatedUser = authenticatedUserService.validateAdmin();
        Client partner = clientService.findEntityById(request.getPartnerId());

        if (!Role.PARTNER.equals(partner.getRole())) {
            throw new ResourceNotFoundException(MonitorValidationMessages.CLIENT_NOT_PARTNER);
        }

        geolocationService.getMonitorCoordinates(request);

        List<AdvertisingAttachment> advertisingAttachments = getAdvertisingAttachments(request);
        Set<Client> clients = getClients(advertisingAttachments);

        Monitor monitor;

        if (monitorId != null) {
            monitor = findEntityById(monitorId);
            boolean addressChanged = monitor.getAddress().hasChanged(request.getAddress());

            if (addressChanged) {
                geolocationService.getMonitorCoordinates(request);
            }

            CustomRevisionListener.setUsername(authenticatedUser.client().getBusinessName());
            CustomRevisionListener.setOldData(monitor.toStringMapper());
            update(request, monitor, partner, clients, advertisingAttachments);
            monitor.setUsernameUpdate(authenticatedUser.client().getBusinessName());
        } else {
            geolocationService.getMonitorCoordinates(request);
            monitor = new Monitor(request, partner, clients, advertisingAttachments);
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
                .filter(attachment -> AttachmentValidationType.APPROVED.equals(attachment.getValidation()))
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

    private Set<Client> getClients(List<AdvertisingAttachment> advertisingAttachments) {
        return advertisingAttachments.isEmpty() ? Set.of() : advertisingAttachments.stream()
                .map(AdvertisingAttachment::getClient)
                .collect(Collectors.toSet());
    }

    private List<AdvertisingAttachment> getAdvertisingAttachments(MonitorRequestDto request) {
        List<AdvertisingAttachment> advertisingAttachments = advertisingAttachmentRepository.findAllById(
                ValidateDataUtils.isNullOrEmpty(request.getAdvertisingAttachmentsIds())
                        ? List.of()
                        : request.getAdvertisingAttachmentsIds()
        ).orElseThrow(() -> new ResourceNotFoundException(AttachmentValidationMessages.ATTACHMENT_NOT_FOUND));

        advertisingAttachments.removeIf(attachment -> !AttachmentValidationType.APPROVED.equals(attachment.getValidation()));
        return advertisingAttachments;
    }

    private void update(MonitorRequestDto request, Monitor monitor, Client partner, Set<Client> clients, List<AdvertisingAttachment> advertisingAttachments) {
        monitor.setType(request.getType());
        monitor.setLocationDescription(request.getLocationDescription());
        monitor.setMaxBlocks(request.getMaxBlocks());
        monitor.setSize(request.getSize());
        monitor.setLatitude(request.getLatitude());
        monitor.setLongitude(request.getLongitude());
        monitor.getAddress().update(request.getAddress());

        monitor.getAdvertisingAttachments().clear();
        monitor.getAdvertisingAttachments().addAll(advertisingAttachments);

        monitor.getClients().clear();
        monitor.getClients().addAll(clients);

        if (!monitor.getPartner().getId().equals(request.getPartnerId())) {
            monitor.setPartner(partner);
        }
    }

    Monitor findEntityById(UUID monitorId) {
        authenticatedUserService.validateAdmin();
        return repository.findById(monitorId).orElseThrow(() -> new ResourceNotFoundException(MonitorValidationMessages.MONITOR_NOT_FOUND));
    }
}
