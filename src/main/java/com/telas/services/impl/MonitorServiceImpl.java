package com.telas.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.telas.dtos.request.MonitorAdvertisingAttachmentRequestDto;
import com.telas.dtos.request.MonitorRequestDto;
import com.telas.dtos.response.MonitorAdvertisingAttachmentResponseDto;
import com.telas.dtos.response.MonitorMinResponseDto;
import com.telas.dtos.response.MonitorResponseDto;
import com.telas.entities.*;
import com.telas.enums.AttachmentValidationType;
import com.telas.enums.Role;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.infra.security.model.AuthenticatedUser;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.repositories.AdvertisingAttachmentRepository;
import com.telas.repositories.MonitorAdvertisingAttachmentRepository;
import com.telas.repositories.MonitorRepository;
import com.telas.services.BucketService;
import com.telas.services.ClientService;
import com.telas.services.GeolocationService;
import com.telas.services.MonitorService;
import com.telas.shared.audit.CustomRevisionListener;
import com.telas.shared.constants.valitation.AttachmentValidationMessages;
import com.telas.shared.constants.valitation.MonitorValidationMessages;
import com.telas.shared.utils.AttachmentUtils;
import com.telas.shared.utils.ValidateDataUtils;
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
    private final MonitorAdvertisingAttachmentRepository monitorAdvertisingAttachmentRepository;
    private final GeolocationService geolocationService;
    private final AdvertisingAttachmentRepository advertisingAttachmentRepository;
    private final BucketService bucketService;
    private final ClientService clientService;

    @Override
    @Transactional
    public void save(MonitorRequestDto request, UUID monitorId) throws JsonProcessingException {
        AuthenticatedUser authenticatedUser = authenticatedUserService.validateAdmin();
        request.validadeAdvertisingAttachmentsOrderIndex();
        Client partner = clientService.findEntityById(request.getPartnerId());

        if (!Role.PARTNER.equals(partner.getRole())) {
            throw new ResourceNotFoundException(MonitorValidationMessages.CLIENT_NOT_PARTNER);
        }

        geolocationService.getMonitorCoordinates(request);

        List<AdvertisingAttachment> advertisingAttachments = getAdvertisingAttachments(request);
        Set<Client> clients = getClients(advertisingAttachments);

        Monitor monitor = (monitorId != null) ? updateExistingMonitor(request, monitorId, authenticatedUser, partner, clients, advertisingAttachments)
                : createNewMonitor(request, authenticatedUser, partner, clients, advertisingAttachments);

        repository.save(monitor);

        if (monitorId == null) {
            monitorAdvertisingAttachmentRepository.saveAll(monitor.getMonitorAdvertisingAttachments());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public MonitorResponseDto findById(UUID monitorId) {
        authenticatedUserService.validateAdmin();

        Monitor entity = repository.findById(monitorId).orElseThrow(() -> new ResourceNotFoundException(MonitorValidationMessages.MONITOR_NOT_FOUND));

        List<MonitorAdvertisingAttachmentResponseDto> advertisingAttachmentLinks = entity.getMonitorAdvertisingAttachments().stream()
                .filter(monitorAttachment -> AttachmentValidationType.APPROVED.equals(monitorAttachment.getAdvertisingAttachment().getValidation()))
                .map(monitorAttachment -> new MonitorAdvertisingAttachmentResponseDto(monitorAttachment, bucketService.getLink(AttachmentUtils.format(monitorAttachment.getAdvertisingAttachment()))))
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

    private Monitor createNewMonitor(MonitorRequestDto request, AuthenticatedUser authenticatedUser, Client partner, Set<Client> clients, List<AdvertisingAttachment> advertisingAttachments) {
        geolocationService.getMonitorCoordinates(request);
        Monitor monitor = new Monitor(request, partner, clients, advertisingAttachments);
        monitor.setUsernameCreate(authenticatedUser.client().getBusinessName());
        return monitor;
    }

    private Monitor updateExistingMonitor(MonitorRequestDto request, UUID monitorId, AuthenticatedUser authenticatedUser, Client partner, Set<Client> clients, List<AdvertisingAttachment> advertisingAttachments) throws JsonProcessingException {
        Monitor monitor = findEntityById(monitorId);

        if (monitor.getAddress().hasChanged(request.getAddress())) {
            geolocationService.getMonitorCoordinates(request);
        }

        CustomRevisionListener.setUsername(authenticatedUser.client().getBusinessName());
        CustomRevisionListener.setOldData(monitor.toStringMapper());
        updateMonitorDetails(request, monitor, partner, clients, advertisingAttachments);
        monitor.setUsernameUpdate(authenticatedUser.client().getBusinessName());

        return monitor;
    }

    private void updateMonitorDetails(MonitorRequestDto request, Monitor monitor, Client partner, Set<Client> clients, List<AdvertisingAttachment> advertisingAttachments) {
        monitor.setType(request.getType());
        monitor.setLocationDescription(request.getLocationDescription());
        monitor.setMaxBlocks(request.getMaxBlocks());
        monitor.setSize(request.getSize());
        monitor.setLatitude(request.getLatitude());
        monitor.setLongitude(request.getLongitude());
        monitor.getAddress().update(request.getAddress());

        if (!ValidateDataUtils.isNullOrEmpty(advertisingAttachments)) {
            monitorAdvertisingAttachmentRepository.deleteAll(monitor.getMonitorAdvertisingAttachments());
            monitor.getMonitorAdvertisingAttachments().clear();

            advertisingAttachments.forEach(attachment -> monitor.getMonitorAdvertisingAttachments().add(
                    new MonitorAdvertisingAttachment(request.getAdvertisingAttachments().get(advertisingAttachments.indexOf(attachment)), monitor, attachment)
            ));

            monitorAdvertisingAttachmentRepository.saveAll(monitor.getMonitorAdvertisingAttachments());
        }

        monitor.getClients().clear();
        monitor.getClients().addAll(clients);

        if (!monitor.getPartner().getId().equals(request.getPartnerId())) {
            monitor.setPartner(partner);
        }
    }

    private Set<Client> getClients(List<AdvertisingAttachment> advertisingAttachments) {
        return advertisingAttachments.isEmpty() ? Set.of() : advertisingAttachments.stream()
                .map(AdvertisingAttachment::getClient)
                .collect(Collectors.toSet());
    }

    private List<AdvertisingAttachment> getAdvertisingAttachments(MonitorRequestDto request) {
        List<UUID> advertisingAttachmentIds = request.getAdvertisingAttachments().stream()
                .map(MonitorAdvertisingAttachmentRequestDto::getId)
                .toList();

        List<AdvertisingAttachment> advertisingAttachments = advertisingAttachmentRepository.findAllById(
                ValidateDataUtils.isNullOrEmpty(advertisingAttachmentIds)
                        ? List.of()
                        : advertisingAttachmentIds
        ).orElseThrow(() -> new ResourceNotFoundException(AttachmentValidationMessages.ATTACHMENT_NOT_FOUND));

        advertisingAttachments.removeIf(attachment -> !AttachmentValidationType.APPROVED.equals(attachment.getValidation()));
        return advertisingAttachments;
    }

    Monitor findEntityById(UUID monitorId) {
        authenticatedUserService.validateAdmin();
        return repository.findById(monitorId).orElseThrow(() -> new ResourceNotFoundException(MonitorValidationMessages.MONITOR_NOT_FOUND));
    }
}
