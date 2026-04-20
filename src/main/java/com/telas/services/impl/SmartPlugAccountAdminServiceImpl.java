package com.telas.services.impl;

import com.telas.dtos.request.SmartPlugAccountCreateRequestDto;
import com.telas.dtos.request.SmartPlugAccountUpdateRequestDto;
import com.telas.dtos.response.SmartPlugAccountResponseDto;
import com.telas.entities.Box;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.monitoring.crypto.AesTextEncryptionService;
import com.telas.monitoring.entities.SmartPlugAccountEntity;
import com.telas.monitoring.repositories.SmartPlugAccountEntityRepository;
import com.telas.repositories.BoxRepository;
import com.telas.services.SmartPlugAccountAdminService;
import com.telas.shared.constants.valitation.BoxValidationMessages;
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
public class SmartPlugAccountAdminServiceImpl implements SmartPlugAccountAdminService {

    private static final String VENDOR_KASA = "KASA";
    private static final String VENDOR_TAPO = "TAPO";
    private static final String VENDOR_TPLINK = "TPLINK";

    private final SmartPlugAccountEntityRepository smartPlugAccountEntityRepository;
    private final BoxRepository boxRepository;
    private final AesTextEncryptionService encryptionService;

    @Override
    @Transactional(readOnly = true)
    public List<SmartPlugAccountResponseDto> listByBox(UUID boxId) {
        ensureBoxExists(boxId);
        return smartPlugAccountEntityRepository.findByBox_IdOrderByVendorAsc(boxId).stream()
                .map(SmartPlugAccountResponseDto::new)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SmartPlugAccountResponseDto getById(UUID id) {
        SmartPlugAccountEntity entity =
                smartPlugAccountEntityRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                MonitoringValidationMessages.SMART_PLUG_ACCOUNT_NOT_FOUND));
        return new SmartPlugAccountResponseDto(entity);
    }

    @Override
    @Transactional
    public SmartPlugAccountResponseDto create(SmartPlugAccountCreateRequestDto dto) {
        Box box =
                boxRepository
                        .findById(dto.getBoxId())
                        .orElseThrow(() -> new ResourceNotFoundException(BoxValidationMessages.BOX_NOT_FOUND));
        String vendor = normalizeVendor(dto.getVendor());
        validateVendor(vendor);
        if (smartPlugAccountEntityRepository.findByBox_IdAndVendor(box.getId(), vendor).isPresent()) {
            throw new BusinessRuleException(
                    MonitoringValidationMessages.SMART_PLUG_ACCOUNT_BOX_VENDOR_DUPLICATE);
        }
        SmartPlugAccountEntity entity = new SmartPlugAccountEntity();
        entity.setBox(box);
        entity.setVendor(vendor);
        entity.setAccountEmail(dto.getAccountEmail());
        entity.setEnabled(dto.getEnabled() == null || Boolean.TRUE.equals(dto.getEnabled()));
        applyPassword(entity, dto.getPassword(), true);
        Instant now = Instant.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        smartPlugAccountEntityRepository.save(entity);
        return new SmartPlugAccountResponseDto(entity);
    }

    @Override
    @Transactional
    public SmartPlugAccountResponseDto update(UUID id, SmartPlugAccountUpdateRequestDto dto) {
        SmartPlugAccountEntity entity =
                smartPlugAccountEntityRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                MonitoringValidationMessages.SMART_PLUG_ACCOUNT_NOT_FOUND));
        if (dto.getAccountEmail() != null) {
            entity.setAccountEmail(dto.getAccountEmail());
        }
        if (dto.getEnabled() != null) {
            entity.setEnabled(dto.getEnabled());
        }
        applyPassword(entity, dto.getPassword(), false);
        entity.setUpdatedAt(Instant.now());
        smartPlugAccountEntityRepository.save(entity);
        return new SmartPlugAccountResponseDto(entity);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        SmartPlugAccountEntity entity =
                smartPlugAccountEntityRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                MonitoringValidationMessages.SMART_PLUG_ACCOUNT_NOT_FOUND));
        smartPlugAccountEntityRepository.delete(entity);
    }

    private void ensureBoxExists(UUID boxId) {
        if (!boxRepository.existsById(boxId)) {
            throw new ResourceNotFoundException(BoxValidationMessages.BOX_NOT_FOUND);
        }
    }

    private static String normalizeVendor(String raw) {
        return raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
    }

    private static void validateVendor(String vendor) {
        if (!VENDOR_KASA.equals(vendor) && !VENDOR_TAPO.equals(vendor) && !VENDOR_TPLINK.equals(vendor)) {
            throw new BusinessRuleException("Vendor must be KASA, TAPO or TPLINK.");
        }
    }

    private void applyPassword(SmartPlugAccountEntity entity, String password, boolean creating) {
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
}
