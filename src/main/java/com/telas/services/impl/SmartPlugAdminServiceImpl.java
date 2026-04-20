package com.telas.services.impl;

import com.telas.dtos.request.SmartPlugInventoryRequestDto;
import com.telas.dtos.request.SmartPlugRequestDto;
import com.telas.dtos.request.SmartPlugUpdateRequestDto;
import com.telas.dtos.response.SmartPlugReadingResponseDto;
import com.telas.dtos.response.SmartPlugResponseDto;
import com.telas.entities.Box;
import com.telas.entities.Monitor;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.monitoring.crypto.AesTextEncryptionService;
import com.telas.monitoring.entities.SmartPlugEntity;
import com.telas.monitoring.plug.PlugReading;
import com.telas.monitoring.plug.SmartPlugClient;
import com.telas.monitoring.plug.SmartPlugCredentials;
import com.telas.monitoring.repositories.SmartPlugEntityRepository;
import com.telas.repositories.BoxRepository;
import com.telas.repositories.MonitorRepository;
import com.telas.services.SmartPlugAdminService;
import com.telas.shared.constants.valitation.BoxValidationMessages;
import com.telas.shared.constants.valitation.MonitorValidationMessages;
import com.telas.shared.constants.valitation.MonitoringValidationMessages;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SmartPlugAdminServiceImpl implements SmartPlugAdminService {

    private final SmartPlugEntityRepository smartPlugEntityRepository;
    private final MonitorRepository monitorRepository;
    private final BoxRepository boxRepository;
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
    @Transactional(readOnly = true)
    public List<SmartPlugResponseDto> findUnassignedInventory(UUID forMonitorId, UUID forBoxId) {
        List<SmartPlugEntity> unassigned =
                smartPlugEntityRepository.findByMonitorIsNullAndBoxIsNullOrderByCreatedAtDesc();
        List<SmartPlugEntity> result = new ArrayList<>();
        if (forMonitorId != null) {
            smartPlugEntityRepository.findByMonitor_Id(forMonitorId).ifPresent(result::add);
        }
        if (forBoxId != null) {
            smartPlugEntityRepository
                    .findByBox_Id(forBoxId)
                    .ifPresent(
                            p -> {
                                if (result.stream().noneMatch(r -> r.getId().equals(p.getId()))) {
                                    result.add(p);
                                }
                            });
        }
        for (SmartPlugEntity p : unassigned) {
            if (result.stream().noneMatch(r -> r.getId().equals(p.getId()))) {
                result.add(p);
            }
        }
        return result.stream().map(SmartPlugResponseDto::new).collect(Collectors.toList());
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
        entity.setBox(null);
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
    public SmartPlugResponseDto createInventory(SmartPlugInventoryRequestDto dto) {
        String mac = normalizeMac(dto.getMacAddress());
        if (smartPlugEntityRepository.findByMacAddress(mac).isPresent()) {
            throw new BusinessRuleException(MonitoringValidationMessages.SMART_PLUG_MAC_DUPLICATE);
        }
        SmartPlugEntity entity = new SmartPlugEntity();
        entity.setMacAddress(mac);
        entity.setVendor(dto.getVendor());
        entity.setModel(dto.getModel());
        entity.setDisplayName(dto.getDisplayName());
        entity.setAccountEmail(dto.getAccountEmail());
        entity.setMonitor(null);
        entity.setBox(null);
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
    public SmartPlugResponseDto assignToMonitor(UUID plugId, UUID monitorId) {
        SmartPlugEntity plug =
                smartPlugEntityRepository
                        .findById(plugId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                MonitoringValidationMessages.SMART_PLUG_NOT_FOUND));
        Monitor monitor =
                monitorRepository
                        .findById(monitorId)
                        .orElseThrow(() -> new ResourceNotFoundException(MonitorValidationMessages.MONITOR_NOT_FOUND));
        if (plug.getMonitor() != null && plug.getMonitor().getId().equals(monitorId)) {
            return new SmartPlugResponseDto(plug);
        }
        smartPlugEntityRepository
                .findByMonitor_Id(monitorId)
                .filter(other -> !other.getId().equals(plugId))
                .ifPresent(
                        x -> {
                            throw new BusinessRuleException(
                                    MonitoringValidationMessages.SMART_PLUG_MONITOR_ALREADY_LINKED);
                        });
        plug.setMonitor(monitor);
        plug.setBox(null);
        plug.setUpdatedAt(Instant.now());
        smartPlugEntityRepository.save(plug);
        return new SmartPlugResponseDto(plug);
    }

    @Override
    @Transactional
    public SmartPlugResponseDto assignToBox(UUID plugId, UUID boxId) {
        SmartPlugEntity plug =
                smartPlugEntityRepository
                        .findById(plugId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                MonitoringValidationMessages.SMART_PLUG_NOT_FOUND));
        Box box =
                boxRepository
                        .findById(boxId)
                        .orElseThrow(() -> new ResourceNotFoundException(BoxValidationMessages.BOX_NOT_FOUND));
        if (plug.getBox() != null && plug.getBox().getId().equals(boxId)) {
            return new SmartPlugResponseDto(plug);
        }
        smartPlugEntityRepository
                .findByBox_Id(boxId)
                .filter(other -> !other.getId().equals(plugId))
                .ifPresent(
                        x -> {
                            throw new BusinessRuleException(
                                    MonitoringValidationMessages.SMART_PLUG_BOX_ALREADY_LINKED);
                        });
        plug.setBox(box);
        plug.setMonitor(null);
        plug.setUpdatedAt(Instant.now());
        smartPlugEntityRepository.save(plug);
        return new SmartPlugResponseDto(plug);
    }

    @Override
    @Transactional
    public SmartPlugResponseDto unassign(UUID plugId) {
        SmartPlugEntity plug =
                smartPlugEntityRepository
                        .findById(plugId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                MonitoringValidationMessages.SMART_PLUG_NOT_FOUND));
        plug.setMonitor(null);
        plug.setBox(null);
        plug.setUpdatedAt(Instant.now());
        smartPlugEntityRepository.save(plug);
        return new SmartPlugResponseDto(plug);
    }

    @Override
    @Transactional
    public SmartPlugResponseDto update(UUID id, SmartPlugUpdateRequestDto dto) {
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
        SmartPlugCredentials creds = null;
        if (entity.getAccountEmail() != null && !entity.getAccountEmail().isBlank()) {
            creds = new SmartPlugCredentials(entity.getAccountEmail(), password);
        } else if (password != null && !password.isBlank()) {
            creds = new SmartPlugCredentials(null, password);
        }
        PlugReading reading = smartPlugClient.read(entity, creds);
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
