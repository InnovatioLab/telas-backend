package com.telas.services.impl;

import com.telas.dtos.response.EmailAlertCategoryOptionDto;
import com.telas.dtos.response.EmailAlertPreferencesResponseDto;
import com.telas.entities.AdminEmailAlertPreference;
import com.telas.entities.Client;
import com.telas.enums.AdminEmailAlertCategory;
import com.telas.enums.Role;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.repositories.AdminEmailAlertPreferenceRepository;
import com.telas.repositories.ClientRepository;
import com.telas.services.AdminEmailAlertPreferenceService;
import com.telas.shared.constants.valitation.ClientValidationMessages;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminEmailAlertPreferenceServiceImpl implements AdminEmailAlertPreferenceService {

    private final AdminEmailAlertPreferenceRepository preferenceRepository;
    private final ClientRepository clientRepository;

    @Override
    @Transactional
    public void ensureDefaultEmailPreferencesForAdmin(UUID clientId) {
        Client client =
                clientRepository
                        .findById(clientId)
                        .orElseThrow(() -> new ResourceNotFoundException(ClientValidationMessages.USER_NOT_FOUND));
        if (!Role.ADMIN.equals(client.getRole())) {
            return;
        }
        if (preferenceRepository
                .findByClient_IdAndAlertCategory(clientId, AdminEmailAlertCategory.BOX_HEARTBEAT_CONNECTIVITY.name())
                .isPresent()) {
            return;
        }
        AdminEmailAlertPreference row = new AdminEmailAlertPreference();
        row.setClient(client);
        row.setAlertCategory(AdminEmailAlertCategory.BOX_HEARTBEAT_CONNECTIVITY.name());
        row.setEnabled(true);
        preferenceRepository.save(row);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean wantsEmail(UUID clientId, AdminEmailAlertCategory category) {
        Optional<AdminEmailAlertPreference> row =
                preferenceRepository.findByClient_IdAndAlertCategory(clientId, category.name());
        if (row.isPresent()) {
            return row.get().isEnabled();
        }
        return clientRepository
                .findById(clientId)
                .map(c -> Role.DEVELOPER.equals(c.getRole()))
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public EnumMap<AdminEmailAlertCategory, Boolean> getPreferencesForAdmin(UUID clientId) {
        Client client =
                clientRepository
                        .findById(clientId)
                        .orElseThrow(() -> new ResourceNotFoundException(ClientValidationMessages.USER_NOT_FOUND));
        if (!Role.ADMIN.equals(client.getRole())) {
            throw new IllegalArgumentException("Email alert preferences apply only to ADMIN users.");
        }
        EnumMap<AdminEmailAlertCategory, Boolean> map = new EnumMap<>(AdminEmailAlertCategory.class);
        for (AdminEmailAlertCategory c : AdminEmailAlertCategory.values()) {
            map.put(c, false);
        }
        preferenceRepository.findAllByClient_Id(clientId).forEach(
                row -> {
                    try {
                        map.put(AdminEmailAlertCategory.valueOf(row.getAlertCategory()), row.isEnabled());
                    } catch (IllegalArgumentException ignored) {
                    }
                });
        return map;
    }

    @Override
    @Transactional(readOnly = true)
    public EmailAlertPreferencesResponseDto getPreferencesResponseForAdmin(UUID clientId) {
        EnumMap<AdminEmailAlertCategory, Boolean> map = getPreferencesForAdmin(clientId);
        Map<String, Boolean> out = new LinkedHashMap<>();
        for (AdminEmailAlertCategory c : AdminEmailAlertCategory.values()) {
            out.put(c.name(), Boolean.TRUE.equals(map.get(c)));
        }
        return EmailAlertPreferencesResponseDto.builder().preferences(out).build();
    }

    @Override
    @Transactional
    public void replaceFromRequest(UUID targetClientId, Map<String, Boolean> raw) {
        EnumMap<AdminEmailAlertCategory, Boolean> map = new EnumMap<>(AdminEmailAlertCategory.class);
        for (AdminEmailAlertCategory c : AdminEmailAlertCategory.values()) {
            map.put(c, raw != null && Boolean.TRUE.equals(raw.get(c.name())));
        }
        replacePreferencesForAdmin(targetClientId, map);
    }

    @Override
    public List<EmailAlertCategoryOptionDto> getCatalog() {
        List<EmailAlertCategoryOptionDto> list = new ArrayList<>();
        for (AdminEmailAlertCategory c : AdminEmailAlertCategory.values()) {
            list.add(new EmailAlertCategoryOptionDto(c.name(), labelEn(c)));
        }
        return list;
    }

    private static String labelEn(AdminEmailAlertCategory c) {
        return switch (c) {
            case BOX_HEARTBEAT_CONNECTIVITY -> "Box heartbeat / connectivity";
            case SMART_PLUG_UNREACHABLE_OR_POWER -> "Smart plug — unreachable or power loss";
            case SMART_PLUG_RELAY_OFF -> "Smart plug — relay off (manual off)";
            case HOST_REBOOT -> "Host reboot detected";
        };
    }

    @Override
    @Transactional
    public void replacePreferencesForAdmin(UUID targetClientId, EnumMap<AdminEmailAlertCategory, Boolean> preferences) {
        Client target =
                clientRepository
                        .findById(targetClientId)
                        .orElseThrow(() -> new ResourceNotFoundException(ClientValidationMessages.USER_NOT_FOUND));
        if (!Role.ADMIN.equals(target.getRole())) {
            throw new IllegalArgumentException("Email alert preferences apply only to ADMIN users.");
        }
        preferenceRepository.deleteByClient_Id(targetClientId);
        if (preferences == null || preferences.isEmpty()) {
            return;
        }
        for (var e : preferences.entrySet()) {
            if (Boolean.TRUE.equals(e.getValue())) {
                AdminEmailAlertPreference row = new AdminEmailAlertPreference();
                row.setClient(target);
                row.setAlertCategory(e.getKey().name());
                row.setEnabled(true);
                preferenceRepository.save(row);
            }
        }
    }
}
