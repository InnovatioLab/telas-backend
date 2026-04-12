package com.telas.services.impl;

import com.telas.dtos.request.SmartPlugRequestDto;
import com.telas.dtos.response.SmartPlugReadingResponseDto;
import com.telas.dtos.response.SmartPlugResponseDto;
import com.telas.entities.Monitor;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.monitoring.crypto.AesTextEncryptionService;
import com.telas.monitoring.entities.SmartPlugEntity;
import com.telas.monitoring.plug.PlugReading;
import com.telas.monitoring.plug.SmartPlugClient;
import com.telas.monitoring.repositories.SmartPlugEntityRepository;
import com.telas.repositories.MonitorRepository;
import com.telas.services.SmartPlugAdminService;
import com.telas.shared.constants.valitation.MonitorValidationMessages;
import com.telas.shared.constants.valitation.MonitoringValidationMessages;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SmartPlugAdminServiceImpl implements SmartPlugAdminService {

    private final SmartPlugEntityRepository smartPlugEntityRepository;
    private final MonitorRepository monitorRepository;
    private final AesTextEncryptionService encryptionService;
    private final SmartPlugClient smartPlugClient;

    @Override
    @Transactional(readOnly = true)
    public List<SmartPlugResponseDto> findAll() {
        return smartPlugEntityRepository.findAllWithMonitor().stream()
                .map(SmartPlugResponseDto::new)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SmartPlugResponseDto create(SmartPlugRequestDto dto) {
        String mac = normalizeMac(dto.getMacAddress());
        if (smartPlugEntityRepository.findByMacAddress(mac).isPresent()) {
            throw new BusinessRuleException(MonitoringValidationMessages.SMART_PLUG_MAC_DUPLICATE);
        }
        Monitor monitor =
                monitorRepository
                        .findById(dto.getMonitorId())
                        .orElseThrow(() -> new ResourceNotFoundException(MonitorValidationMessages.MONITOR_NOT_FOUND));
        if (smartPlugEntityRepository.findByMonitor_Id(monitor.getId()).isPresent()) {
            throw new BusinessRuleException(MonitoringValidationMessages.SMART_PLUG_MONITOR_ALREADY_LINKED);
        }
        SmartPlugEntity entity = new SmartPlugEntity();
        entity.setMacAddress(mac);
        entity.setVendor(dto.getVendor());
        entity.setModel(dto.getModel());
        entity.setDisplayName(dto.getDisplayName());
        entity.setAccountEmail(dto.getAccountEmail());
        entity.setMonitor(monitor);
        entity.setEnabled(Boolean.TRUE.equals(dto.getEnabled()));
        entity.setLastSeenIp(dto.getLastSeenIp());
        applyPassword(entity, dto.getPassword(), true);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        smartPlugEntityRepository.save(entity);
        return new SmartPlugResponseDto(entity);
    }

    @Override
    @Transactional
    public SmartPlugResponseDto update(UUID id, SmartPlugRequestDto dto) {
        SmartPlugEntity entity =
                smartPlugEntityRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                MonitoringValidationMessages.SMART_PLUG_NOT_FOUND));
        String mac = normalizeMac(dto.getMacAddress());
        smartPlugEntityRepository
                .findByMacAddress(mac)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(
                        x -> {
                            throw new BusinessRuleException(
                                    MonitoringValidationMessages.SMART_PLUG_MAC_DUPLICATE);
                        });
        if (!entity.getMonitor().getId().equals(dto.getMonitorId())) {
            Monitor monitor =
                    monitorRepository
                            .findById(dto.getMonitorId())
                            .orElseThrow(
                                    () ->
                                            new ResourceNotFoundException(
                                                    MonitorValidationMessages.MONITOR_NOT_FOUND));
            smartPlugEntityRepository
                    .findByMonitor_Id(monitor.getId())
                    .filter(other -> !other.getId().equals(id))
                    .ifPresent(
                            x -> {
                                throw new BusinessRuleException(
                                        MonitoringValidationMessages.SMART_PLUG_MONITOR_ALREADY_LINKED);
                            });
            entity.setMonitor(monitor);
        }
        entity.setMacAddress(mac);
        entity.setVendor(dto.getVendor());
        entity.setModel(dto.getModel());
        entity.setDisplayName(dto.getDisplayName());
        entity.setAccountEmail(dto.getAccountEmail());
        if (dto.getEnabled() != null) {
            entity.setEnabled(dto.getEnabled());
        }
        if (dto.getLastSeenIp() != null) {
            entity.setLastSeenIp(dto.getLastSeenIp());
        }
        applyPassword(entity, dto.getPassword(), false);
        entity.setUpdatedAt(Instant.now());
        smartPlugEntityRepository.save(entity);
        return new SmartPlugResponseDto(entity);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        SmartPlugEntity entity =
                smartPlugEntityRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                MonitoringValidationMessages.SMART_PLUG_NOT_FOUND));
        smartPlugEntityRepository.delete(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public SmartPlugReadingResponseDto testRead(UUID id) {
        SmartPlugEntity entity =
                smartPlugEntityRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                MonitoringValidationMessages.SMART_PLUG_NOT_FOUND));
        String password = null;
        if (encryptionService.isConfigured() && entity.getPasswordCipher() != null) {
            password = encryptionService.decrypt(entity.getPasswordCipher());
        }
        PlugReading reading = smartPlugClient.read(entity, password);
        return new SmartPlugReadingResponseDto(reading);
    }

    private void applyPassword(SmartPlugEntity entity, String password, boolean creating) {
        if (!StringUtils.hasText(password)) {
            if (creating) {
                entity.setPasswordCipher(null);
            }
            return;
        }
        if (!encryptionService.isConfigured()) {
            throw new BusinessRuleException(MonitoringValidationMessages.SMART_PLUG_ENCRYPTION_REQUIRED);
        }
        entity.setPasswordCipher(encryptionService.encrypt(password));
    }

    private static String normalizeMac(String raw) {
        return raw.replaceAll("[:-]", "").toUpperCase(Locale.ROOT);
    }
}
