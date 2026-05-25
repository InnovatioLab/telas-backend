package com.telas.services.impl;

import com.telas.dtos.request.UpdateEmailAlertPreferencesRequestDto;
import com.telas.dtos.request.UpdatePartnerPlatformSettingsRequestDto;
import com.telas.dtos.request.UpdatePermissionsRequestDto;
import com.telas.dtos.response.AdminPermissionRowResponseDto;
import com.telas.dtos.response.EmailAlertCategoryOptionDto;
import com.telas.dtos.response.EmailAlertPreferencesResponseDto;
import com.telas.dtos.response.PartnerPlatformSettingsResponseDto;
import com.telas.entities.Client;
import com.telas.enums.Permission;
import com.telas.repositories.ClientRepository;
import com.telas.services.AdminEmailAlertPreferenceService;
import com.telas.services.DeveloperAdminService;
import com.telas.services.PartnerPlatformSettingsService;
import com.telas.services.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeveloperAdminServiceImpl implements DeveloperAdminService {

    private final ClientRepository clientRepository;
    private final PermissionService permissionService;
    private final PartnerPlatformSettingsService partnerPlatformSettingsService;
    private final AdminEmailAlertPreferenceService adminEmailAlertPreferenceService;

    @Override
    @Transactional(readOnly = true)
    public List<AdminPermissionRowResponseDto> listAdminsWithPermissions() {
        List<Client> admins = clientRepository.findAllAdmins();
        return admins.stream()
                .map(a -> new AdminPermissionRowResponseDto(
                        a.getId(),
                        a.getBusinessName(),
                        a.getContact() != null ? a.getContact().getEmail() : "",
                        permissionService.listPermissionCodesForClient(a.getId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PartnerPlatformSettingsResponseDto getPartnerPlatformSettings() {
        return new PartnerPlatformSettingsResponseDto(
                partnerPlatformSettingsService.isSlotsAnyLocationEnabled(),
                partnerPlatformSettingsService.isAdminCanCreatePartnerEnabled());
    }

    @Override
    @Transactional
    public PartnerPlatformSettingsResponseDto updatePartnerPlatformSettings(UpdatePartnerPlatformSettingsRequestDto body) {
        boolean slotsEnabled =
                partnerPlatformSettingsService.setSlotsAnyLocationEnabled(
                        Boolean.TRUE.equals(body.getPartnerSlotsAnyLocationEnabled()));
        boolean adminCreateEnabled =
                partnerPlatformSettingsService.setAdminCanCreatePartnerEnabled(
                        Boolean.TRUE.equals(body.getAdminCanCreatePartnerEnabled()));
        return new PartnerPlatformSettingsResponseDto(slotsEnabled, adminCreateEnabled);
    }

    @Override
    @Transactional
    public void replacePermissions(UUID clientId, UpdatePermissionsRequestDto body, UUID actorDeveloperId) {
        Set<Permission> parsed = new HashSet<>();
        List<String> raw = body.getPermissions();
        if (raw != null) {
            for (String code : raw) {
                if (code == null || code.isBlank()) {
                    continue;
                }
                String trimmed = code.trim();
                try {
                    parsed.add(Permission.valueOf(trimmed));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        permissionService.replacePermissionsForAdmin(clientId, parsed, actorDeveloperId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> permissionCatalog() {
        return Arrays.stream(Permission.values())
                .map(Enum::name)
                .filter(code -> !"ADMIN_ADS_BOX_DISPATCH_TO_SCREEN".equals(code))
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmailAlertCategoryOptionDto> emailAlertCatalog() {
        return adminEmailAlertPreferenceService.getCatalog();
    }

    @Override
    @Transactional(readOnly = true)
    public EmailAlertPreferencesResponseDto getEmailAlertPreferences(UUID clientId) {
        return adminEmailAlertPreferenceService.getPreferencesResponseForAdmin(clientId);
    }

    @Override
    @Transactional
    public void replaceEmailAlertPreferences(UUID clientId, UpdateEmailAlertPreferencesRequestDto body) {
        adminEmailAlertPreferenceService.replaceFromRequest(clientId, body.getPreferences());
    }
}
